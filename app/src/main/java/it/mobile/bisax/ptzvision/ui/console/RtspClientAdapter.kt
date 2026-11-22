package it.mobile.bisax.ptzvision.ui.console

import android.net.Uri
import android.util.Log
import com.alexvas.rtsp.widget.RtspStatusListener
import com.alexvas.rtsp.widget.RtspSurfaceView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fallback low-latency player based on alexeyvasilyev/rtsp-client-android.
 *
 * Used when the in-house zero-copy path cannot handle a stream (e.g. H.265,
 * Digest authentication). It still has no jitter buffer, decodes to a
 * SurfaceView and rewrites the SPS with low-latency VUI parameters.
 */
class RtspClientAdapter {

    sealed class State {
        object Idle : State()
        object Connecting : State()
        object Playing : State()
        data class Error(val message: String) : State()
    }

    companion object {
        private const val TAG = "RtspClientAdapter"
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val USER_AGENT = "PTZ-Vision-Android"
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var surfaceView: RtspSurfaceView? = null
    // Incremented on every start/stop: callbacks from a stale session are ignored,
    // which prevents the old "disconnected -> restart -> disconnected" loop.
    private var generation = 0

    fun start(
        rtspUrl: String,
        view: RtspSurfaceView,
        username: String = "",
        password: String = "",
    ) {
        stop()
        val gen = ++generation
        surfaceView = view
        _state.value = State.Connecting

        view.debug = false
        view.experimentalUpdateSpsFrameWithLowLatencyParams = true
        view.videoFrameRateStabilization = false // would add buffering
        view.setStatusListener(object : RtspStatusListener {
            override fun onRtspStatusConnecting() { Log.d(TAG, "Connecting…") }
            override fun onRtspStatusConnected() { Log.d(TAG, "Connected") }

            override fun onRtspFirstFrameRendered() {
                if (gen != generation) return
                Log.d(TAG, "First frame rendered")
                _state.value = State.Playing
            }

            override fun onRtspStatusDisconnected() {
                if (gen != generation) return
                Log.d(TAG, "Disconnected")
                scheduleReconnect(gen, rtspUrl, view, username, password)
            }

            override fun onRtspStatusFailedUnauthorized() {
                if (gen != generation) return
                Log.e(TAG, "Authentication failed")
                _state.value = State.Error("Authentication failed")
            }

            override fun onRtspStatusFailed(message: String?) {
                if (gen != generation) return
                Log.e(TAG, "RTSP error: $message")
                _state.value = State.Error(message ?: "Unknown error")
                scheduleReconnect(gen, rtspUrl, view, username, password)
            }
        })

        view.init(Uri.parse(rtspUrl), username, password, USER_AGENT, null)
        view.start(true, false, false)
    }

    fun stop() {
        generation++
        surfaceView?.let { v ->
            v.setStatusListener(null)
            if (v.isStarted()) v.stop()
        }
        surfaceView = null
        _state.value = State.Idle
    }

    private fun scheduleReconnect(
        gen: Int,
        rtspUrl: String,
        view: RtspSurfaceView,
        username: String,
        password: String,
    ) {
        view.postDelayed({
            if (gen == generation) start(rtspUrl, view, username, password)
        }, RECONNECT_DELAY_MS)
    }
}
