package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RtspClientAdapter(
    private val scope: CoroutineScope,
    private val onFirstFrame: () -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private var extractor: RtspNalExtractor? = null
    private var decoder: ZeroCopyMediaCodecDecoder? = null

    fun start(rtspUrl: String, surface: Surface) {
        stop()

        val decoder = ZeroCopyMediaCodecDecoder(
            surface = surface,
            scope = scope,
            onFirstFrame = {
                Log.d(TAG, "▶ First frame rendered")
                onFirstFrame()
            },
            onError = onError
        )
        this.decoder = decoder

        val extractor = RtspNalExtractor(
            rtspUrl = rtspUrl,
            scope = scope,
            onSessionReady = { sessionInfo ->
                decoder.configure(sessionInfo)
            },
            onNalUnit = { nalUnit ->
                decoder.queueNalUnit(nalUnit)
            },
            onError = { error ->
                onError(error)
            }
        )
        this.extractor = extractor
        extractor.start()
    }

    fun stop() {
        extractor?.let { extractor ->
            scope.launch {
                extractor.stop()
            }
        }
        extractor = null
        decoder?.release()
        decoder = null
    }

    private companion object {
        const val TAG = "RtspClientAdapter"
    }
}
