package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/**
 * Hardware H.264 decoder rendering straight into a SurfaceView's Surface.
 *
 * Zero-copy: decoded pictures never leave GPU/kernel memory, the codec's
 * output buffer is handed to SurfaceFlinger (often as a HW overlay) by
 * releaseOutputBuffer(index, true).
 *
 * Low latency:
 *  - one complete access unit per input buffer, queued synchronously from
 *    the network thread (no intermediate queue);
 *  - prefers a decoder advertising FEATURE_LowLatency and enables
 *    KEY_LOW_LATENCY (API 30+) plus the vendor-specific switches used by
 *    Moonlight (Qualcomm, Exynos, Amlogic, HiSilicon) that disable the
 *    output re-ordering / DPB hold-back;
 *  - output frames are rendered as soon as they are dequeued.
 */
class LowLatencyH264Decoder(
    private val surface: Surface,
    private val listener: Listener
) {
    interface Listener {
        fun onVideoSize(width: Int, height: Int)
        fun onFirstFrame()
        fun onStats(stats: Stats)
        fun onDecoderError(message: String, fatal: Boolean)
    }

    data class Stats(
        val decoderName: String,
        val lowLatency: Boolean,
        val width: Int,
        val height: Int,
        val fps: Float,
        /** Average time from queueInputBuffer to frame available, ms. */
        val decodeLatencyMs: Float,
        val droppedInput: Int,
    )

    companion object {
        private const val TAG = "LowLatencyDecoder"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val INPUT_TIMEOUT_US = 100_000L
        private const val OUTPUT_TIMEOUT_US = 20_000L
        private const val MAX_INPUT_SIZE = 4 * 1024 * 1024
    }

    private val lock = Any()
    @Volatile private var codec: MediaCodec? = null
    private var codecName = ""
    private var lowLatencyActive = false
    private var currentSps: ByteArray? = null
    private var currentPps: ByteArray? = null
    private var outputThread: Thread? = null
    @Volatile private var released = false
    @Volatile private var waitingForKeyFrame = true
    @Volatile private var droppedInput = 0
    private var width = 0
    private var height = 0

    /** (Re)configures the codec when the parameter sets change. */
    fun configure(sps: ByteArray, pps: ByteArray) {
        synchronized(lock) {
            if (released) return
            if (codec != null && sps.contentEquals(currentSps) && pps.contentEquals(currentPps)) return
            if (codec != null) Log.d(TAG, "Parameter sets changed, reconfiguring decoder")
            releaseCodecLocked()
            currentSps = sps
            currentPps = pps
            try {
                createCodecLocked(sps, pps)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to create decoder", e)
                listener.onDecoderError("Decoder init failed: ${e.message}", fatal = true)
            }
        }
    }

    /** Feeds one Annex-B access unit. Must be called from a single thread. */
    fun decode(data: ByteArray, size: Int, isKeyFrame: Boolean) {
        val c = synchronized(lock) { codec } ?: return
        if (waitingForKeyFrame && !isKeyFrame) return
        try {
            val index = c.dequeueInputBuffer(INPUT_TIMEOUT_US)
            if (index < 0) {
                // Decoder stalled: dropping a reference frame corrupts the picture,
                // so resume from the next IDR.
                droppedInput++
                waitingForKeyFrame = true
                return
            }
            val buffer: ByteBuffer = c.getInputBuffer(index) ?: return
            buffer.clear()
            if (size > buffer.capacity()) {
                Log.w(TAG, "Access unit too large ($size > ${buffer.capacity()})")
                c.queueInputBuffer(index, 0, 0, 0, 0)
                droppedInput++
                waitingForKeyFrame = true
                return
            }
            buffer.put(data, 0, size)
            val flags = if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            // PTS = arrival time, so output PTS gives us the decode latency for free.
            c.queueInputBuffer(index, 0, size, System.nanoTime() / 1000, flags)
            if (isKeyFrame) waitingForKeyFrame = false
        } catch (e: IllegalStateException) {
            if (!released) {
                Log.w(TAG, "queueInputBuffer failed: ${e.message}")
                listener.onDecoderError("Decoder error: ${e.message}", fatal = false)
            }
        }
    }

    fun release() {
        synchronized(lock) {
            released = true
            releaseCodecLocked()
        }
    }

    // ------------------------------------------------------------------ setup

    private fun createCodecLocked(sps: ByteArray, pps: ByteArray) {
        val info = selectDecoder() ?: throw IllegalStateException("No H.264 decoder")
        val caps = info.getCapabilitiesForType(MIME)
        val supportsLowLatency = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)

        val c = MediaCodec.createByCodecName(info.name)
        var vendorKeys = true
        while (true) {
            val format = buildFormat(sps, pps, info.name, supportsLowLatency, vendorKeys)
            try {
                c.configure(format, surface, null, 0)
                break
            } catch (e: Exception) {
                if (!vendorKeys) {
                    c.release()
                    throw e
                }
                Log.w(TAG, "configure() with vendor keys failed, retrying without: ${e.message}")
                c.reset()
                vendorKeys = false
            }
        }
        c.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        c.start()

        codec = c
        codecName = info.name
        lowLatencyActive = supportsLowLatency || (vendorKeys && hasVendorLowLatency(info.name))
        waitingForKeyFrame = true
        Log.d(TAG, "Decoder ${info.name} started (lowLatency=$lowLatencyActive, vendorKeys=$vendorKeys)")
        startOutputThread(c)
    }

    private fun buildFormat(
        sps: ByteArray,
        pps: ByteArray,
        name: String,
        lowLatency: Boolean,
        vendorKeys: Boolean
    ): MediaFormat {
        // Real size comes from the SPS; this is only a hint.
        return MediaFormat.createVideoFormat(MIME, 1920, 1080).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(sps))
            setByteBuffer("csd-1", ByteBuffer.wrap(pps))
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && lowLatency) {
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            setInteger(MediaFormat.KEY_PRIORITY, 0) // realtime
            // Ask for max clocks (Qualcomm boosts the decoder on this hint).
            setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE.toInt())
            if (vendorKeys) {
                val n = name.lowercase()
                when {
                    n.contains("qcom") || n.contains("qti") -> {
                        setInteger("vendor.qti-ext-dec-picture-order.enable", 1)
                        setInteger("vendor.qti-ext-dec-low-latency.enable", 1)
                    }
                    n.contains("exynos") -> setInteger("vendor.rtc-ext-dec-low-latency.enable", 1)
                    n.contains("amlogic") -> setInteger("vendor.low-latency.enable", 1)
                    n.contains("hisi") -> {
                        setInteger("vendor.hisi-ext-low-latency-video-dec.video-scene-for-low-latency-req", 1)
                        setInteger("vendor.hisi-ext-low-latency-video-dec.video-scene-for-low-latency-rdy", -1)
                    }
                }
            }
        }
    }

    private fun hasVendorLowLatency(name: String): Boolean {
        val n = name.lowercase()
        return listOf("qcom", "qti", "exynos", "amlogic", "hisi").any { n.contains(it) }
    }

    /** Hardware decoder first, low-latency capable first. */
    private fun selectDecoder(): MediaCodecInfo? {
        val candidates = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(MIME, ignoreCase = true) }
        }
        fun isHardware(info: MediaCodecInfo): Boolean = info.isHardwareAccelerated

        fun supportsLowLatency(info: MediaCodecInfo): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                info.getCapabilitiesForType(MIME)
                    .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)

        fun isSecure(info: MediaCodecInfo) = info.name.endsWith(".secure")

        return candidates
            .filterNot(::isSecure)
            .sortedWith(
                compareByDescending<MediaCodecInfo> { isHardware(it) }
                    .thenByDescending { supportsLowLatency(it) }
            )
            .firstOrNull()
    }

    private fun releaseCodecLocked() {
        val c = codec ?: return
        codec = null
        outputThread?.interrupt()
        try {
            c.stop()
        } catch (_: Exception) {
        }
        try {
            c.release()
        } catch (_: Exception) {
        }
        outputThread?.join(500)
        outputThread = null
    }

    // ----------------------------------------------------------------- output

    private fun startOutputThread(c: MediaCodec) {
        outputThread = Thread({
            val info = MediaCodec.BufferInfo()
            var firstFrame = true
            var frames = 0
            var latencySumUs = 0L
            var windowStart = System.nanoTime()

            while (!released && codec === c) {
                val index = try {
                    c.dequeueOutputBuffer(info, OUTPUT_TIMEOUT_US)
                } catch (e: IllegalStateException) {
                    break // codec stopped/released
                }
                when {
                    index >= 0 -> {
                        val nowUs = System.nanoTime() / 1000
                        try {
                            c.releaseOutputBuffer(index, true)
                        } catch (_: IllegalStateException) {
                            break
                        }
                        latencySumUs += (nowUs - info.presentationTimeUs).coerceAtLeast(0)
                        frames++
                        if (firstFrame) {
                            firstFrame = false
                            listener.onFirstFrame()
                        }
                    }
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = c.outputFormat
                        val w = if (f.containsKey("crop-right") && f.containsKey("crop-left"))
                            f.getInteger("crop-right") - f.getInteger("crop-left") + 1
                        else f.getInteger(MediaFormat.KEY_WIDTH)
                        val h = if (f.containsKey("crop-bottom") && f.containsKey("crop-top"))
                            f.getInteger("crop-bottom") - f.getInteger("crop-top") + 1
                        else f.getInteger(MediaFormat.KEY_HEIGHT)
                        width = w
                        height = h
                        Log.d(TAG, "Output format: ${w}x$h")
                        listener.onVideoSize(w, h)
                    }
                }

                val elapsed = System.nanoTime() - windowStart
                if (elapsed >= 1_000_000_000L) {
                    listener.onStats(
                        Stats(
                            decoderName = codecName,
                            lowLatency = lowLatencyActive,
                            width = width,
                            height = height,
                            fps = frames * 1e9f / elapsed,
                            decodeLatencyMs = if (frames > 0) latencySumUs / frames / 1000f else 0f,
                            droppedInput = droppedInput,
                        )
                    )
                    frames = 0
                    latencySumUs = 0
                    windowStart = System.nanoTime()
                }
            }
        }, "LowLatencyDecoder-Out").also {
            it.priority = Thread.MAX_PRIORITY
            it.isDaemon = true
            it.start()
        }
    }
}
