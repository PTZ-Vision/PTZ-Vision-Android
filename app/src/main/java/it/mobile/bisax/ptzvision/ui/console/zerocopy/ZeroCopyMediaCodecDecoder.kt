package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ZeroCopyMediaCodecDecoder
 *
 * Drives the MediaCodec hardware decoder in Surface output mode - the Android
 * equivalent of FFmpeg's h264_mediacodec with AVMediaCodecDeviceContext.surface.
 */
class ZeroCopyMediaCodecDecoder(private val outputSurface: Surface) {

    companion object {
        private const val TAG = "ZeroCopyDecoder"
        private const val MIME = "video/avc"
        private const val TIMEOUT = 10_000L
        private const val MAX_QUEUE = 16
    }

    data class NalUnit(val data: ByteArray, val pts: Long)

    private val inputQueue = ArrayBlockingQueue<NalUnit>(MAX_QUEUE)
    private val running = AtomicBoolean(false)

    private var codec: MediaCodec? = null

    private var spsData: ByteArray? = null
    private var ppsData: ByteArray? = null
    private var codecWidth = 0
    private var codecHeight = 0

    private var inputThread: Thread? = null
    private var outputThread: Thread? = null

    fun feedNalUnit(data: ByteArray, pts: Long) {
        val nalType = if (data.size > 4) (data[4].toInt() and 0x1F) else -1
        when (nalType) {
            7 -> {
                spsData = data
                tryInitCodec()
            }
            8 -> ppsData = data
        }

        if (!running.get()) {
            return
        }

        if (!inputQueue.offer(NalUnit(data, pts))) {
            inputQueue.poll()
            inputQueue.offer(NalUnit(data, pts))
        }
    }

    fun start(width: Int, height: Int) {
        codecWidth = width
        codecHeight = height
        running.set(true)
        tryInitCodec()
        startThreads()
    }

    fun stop() {
        running.set(false)
        inputThread?.interrupt()
        outputThread?.interrupt()
        val current = Thread.currentThread()
        if (inputThread != null && inputThread != current) {
            inputThread?.join(2_000)
        }
        if (outputThread != null && outputThread != current) {
            outputThread?.join(2_000)
        }
        try {
            codec?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "MediaCodec stop failed: ${e.message}")
        }
        codec?.release()
        codec = null
        inputQueue.clear()
    }

    private fun tryInitCodec() {
        if (!running.get()) return
        if (codec != null) return
        if (spsData == null) return
        if (codecWidth <= 0 || codecHeight <= 0) {
            Log.w(TAG, "Skipping MediaCodec init: invalid size ${codecWidth}x${codecHeight}")
            return
        }

        val format = MediaFormat.createVideoFormat(MIME, codecWidth, codecHeight).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            spsData?.let { setByteBuffer("csd-0", java.nio.ByteBuffer.wrap(it)) }
            ppsData?.let { setByteBuffer("csd-1", java.nio.ByteBuffer.wrap(it)) }
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            setInteger(MediaFormat.KEY_OPERATING_RATE, 60)
        }

        try {
            codec = MediaCodec.createDecoderByType(MIME).also { c ->
                c.configure(format, outputSurface, null, 0)
                c.start()
                Log.d(TAG, "MediaCodec configured (Surface output, zero-copy mode)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init MediaCodec: ${e.message}")
            codec = null
        }
    }

    private fun startThreads() {
        inputThread = Thread({
            Log.d(TAG, "Input thread started")
            while (running.get()) {
                val nal = inputQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                val c = codec ?: continue
                val idx = c.dequeueInputBuffer(TIMEOUT)
                if (idx < 0) continue
                val buf = c.getInputBuffer(idx) ?: continue
                buf.clear()
                buf.put(nal.data)
                c.queueInputBuffer(
                    idx,
                    0,
                    nal.data.size,
                    nal.pts,
                    if (nal.isKeyFrame()) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                )
            }
            Log.d(TAG, "Input thread ended")
        }, "ZeroCopy-Input").also { it.priority = Thread.MAX_PRIORITY; it.start() }

        outputThread = Thread({
            Log.d(TAG, "Output thread started")
            val info = MediaCodec.BufferInfo()
            while (running.get()) {
                val c = codec ?: run {
                    Thread.sleep(10)
                    continue
                }
                val idx = c.dequeueOutputBuffer(info, TIMEOUT)
                when {
                    idx >= 0 -> {
                        c.releaseOutputBuffer(idx, true)
                    }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFmt = c.outputFormat
                        Log.d(TAG, "Output format changed: $newFmt")
                    }
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                }
            }
            Log.d(TAG, "Output thread ended")
        }, "ZeroCopy-Output").also { it.priority = Thread.MAX_PRIORITY; it.start() }
    }

    private fun NalUnit.isKeyFrame(): Boolean =
        data.size > 4 && (data[4].toInt() and 0x1F) == 5
}
