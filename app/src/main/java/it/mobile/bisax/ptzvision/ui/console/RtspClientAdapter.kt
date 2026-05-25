package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import com.alexvas.rtsp.RtspClient

/**
 * RtspClientAdapter - zero-buffering RTSP renderer for PTZ-Vision.
 *
 * This adapter wraps alexeyvasilyev/rtsp-client-android, which is the closest
 * Android equivalent to the cctv-viewer approach:
 * - No jitter buffer: frames rendered immediately on arrival
 * - Low-Latency MediaCodec (API 30+) <- AV_CODEC_FLAG_LOW_DELAY equivalent
 * - RTSP/TCP with no UDP fallback penalty
 */
class RtspClientAdapter(private val context: Context) {

    companion object {
        private const val TAG = "RtspClientAdapter"
        private const val RECONNECT_DELAY_MS = 2_000L
    }

    private var rtspClient: RtspClient? = null
    private var isRunning = false

    fun start(
        rtspUrl: String,
        surfaceView: SurfaceView,
        username: String = "",
        password: String = "",
        onFirstFrame: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        stop()
        isRunning = true

        rtspClient = RtspClient.Builder(context)
            .withSurface(surfaceView.holder.surface)
            .withDebug(false)
            .withStatusListener(object : RtspClient.RtspStatusListener {
                override fun onConnecting() {
                    Log.d(TAG, "Connecting...")
                }

                override fun onConnected() {
                    Log.d(TAG, "Connected")
                }

                override fun onFirstFrameRendered() {
                    Log.d(TAG, "First frame rendered")
                    onFirstFrame?.invoke()
                }

                override fun onDisconnected() {
                    Log.d(TAG, "Disconnected")
                    if (isRunning) {
                        surfaceView.postDelayed({
                            if (isRunning) {
                                start(rtspUrl, surfaceView, username, password, onFirstFrame, onError)
                            }
                        }, RECONNECT_DELAY_MS)
                    }
                }

                override fun onUnauthorized() {
                    Log.e(TAG, "Authentication failed")
                    onError?.invoke("Authentication failed for $rtspUrl")
                }

                override fun onFailed(message: String?) {
                    Log.e(TAG, "RTSP error: $message")
                    onError?.invoke(message ?: "Unknown error")
                    if (isRunning) {
                        surfaceView.postDelayed({
                            if (isRunning) {
                                start(rtspUrl, surfaceView, username, password, onFirstFrame, onError)
                            }
                        }, RECONNECT_DELAY_MS)
                    }
                }
            })
            .build()

        if (username.isNotBlank()) {
            rtspClient?.start(rtspUrl, username, password)
        } else {
            rtspClient?.start(rtspUrl)
        }
    }

    fun stop() {
        isRunning = false
        rtspClient?.stop()
        rtspClient = null
    }
}
