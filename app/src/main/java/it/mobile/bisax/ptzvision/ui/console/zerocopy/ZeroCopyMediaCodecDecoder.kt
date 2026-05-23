package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.view.Surface
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ZeroCopyMediaCodecDecoder(
    private val surface: Surface,
    private val scope: CoroutineScope,
    private val onFirstFrame: () -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private var codec: MediaCodec? = null
    private var outputJob: Job? = null
    private var firstFrameRendered = false

    fun configure(sessionInfo: RtspNalExtractor.RtspSessionInfo) {
        release()

        val width = sessionInfo.width.coerceAtLeast(1)
        val height = sessionInfo.height.coerceAtLeast(1)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)

        sessionInfo.sps?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
        sessionInfo.pps?.let { format.setByteBuffer("csd-1", ByteBuffer.wrap(it)) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
        }

        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, surface, null, 0)
            start()
        }

        outputJob = scope.launch(Dispatchers.Default) {
            drainOutput()
        }
    }

    fun queueNalUnit(nalUnit: ByteArray) {
        val codec = codec ?: return
        try {
            val inputIndex = codec.dequeueInputBuffer(INPUT_TIMEOUT_US)
            if (inputIndex >= 0) {
                val buffer = codec.getInputBuffer(inputIndex)
                buffer?.clear()
                buffer?.put(nalUnit)
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    nalUnit.size,
                    SystemClock.elapsedRealtimeNanos() / 1000,
                    0
                )
            }
        } catch (exception: IllegalStateException) {
            onError(exception)
        }
    }

    fun release() {
        outputJob?.cancel()
        outputJob = null
        firstFrameRendered = false
        codec?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        codec = null
    }

    private suspend fun drainOutput() {
        val codec = codec ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            while (isActive) {
                ensureActive()
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, OUTPUT_TIMEOUT_US)
                when {
                    outputIndex >= 0 -> {
                        codec.releaseOutputBuffer(outputIndex, true)
                        if (!firstFrameRendered) {
                            firstFrameRendered = true
                            onFirstFrame()
                        }
                    }
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        delay(IDLE_DELAY_MS)
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Ignore
                    }
                }
            }
        } catch (exception: IllegalStateException) {
            onError(exception)
        }
    }

    private companion object {
        const val INPUT_TIMEOUT_US = 10_000L
        const val OUTPUT_TIMEOUT_US = 10_000L
        const val IDLE_DELAY_MS = 5L
    }
}
