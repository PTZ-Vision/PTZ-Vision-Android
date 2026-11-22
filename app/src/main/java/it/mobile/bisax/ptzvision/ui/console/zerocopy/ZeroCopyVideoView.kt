package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout

/**
 * Low-latency RTSP view: RtspNalExtractor -> LowLatencyH264Decoder -> SurfaceView.
 *
 * The decoder renders directly into the SurfaceView's BufferQueue (no GL
 * pass, no TextureView copy), so SurfaceFlinger can scan the frame out as a
 * hardware overlay: this is the shortest path from network to glass Android
 * offers to an app.
 *
 * Network errors are retried forever with a short back-off; only problems the
 * zero-copy path cannot handle (unsupported codec, no decoder, nothing ever
 * rendered) are reported as [State.Fatal] so the caller can fall back.
 */
class ZeroCopyVideoView(context: Context) : FrameLayout(context), SurfaceHolder.Callback {

    sealed class State {
        object Connecting : State()
        object Playing : State()
        data class Reconnecting(val reason: String) : State()
        data class Fatal(val reason: String) : State()
    }

    interface Listener {
        fun onState(state: State)
    }

    companion object {
        private const val TAG = "ZeroCopyVideoView"
        private const val RECONNECT_MIN_MS = 300L
        private const val RECONNECT_MAX_MS = 3_000L
        private const val NO_FRAME_TIMEOUT_MS = 12_000L
        private const val MAX_FAILED_SESSIONS_WITHOUT_VIDEO = 3
    }

    private val surfaceView = SurfaceView(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var url: String? = null
    private var username = ""
    private var password = ""
    var listener: Listener? = null

    private var surfaceReady = false
    private var extractor: RtspNalExtractor? = null
    private var decoder: LowLatencyH264Decoder? = null
    private var session = 0
    private var reconnectDelay = RECONNECT_MIN_MS
    private var failedSessionsWithoutVideo = 0
    private var everRendered = false
    private var connected = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var fatal = false

    init {
        setBackgroundColor(0xFF000000.toInt())
        addView(surfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surfaceView.holder.addCallback(this)
        keepScreenOn = true
    }

    fun start(url: String, username: String = "", password: String = "") {
        this.url = url
        this.username = username
        this.password = password
        fatal = false
        failedSessionsWithoutVideo = 0
        reconnectDelay = RECONNECT_MIN_MS
        if (surfaceReady) startSession()
    }

    fun stop() {
        url = null
        stopSession()
    }

    // ------------------------------------------------------------- surface

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        if (url != null && !fatal) startSession()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopSession()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    // ------------------------------------------------------------- layout

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = right - left
        val h = bottom - top
        if (videoWidth <= 0 || videoHeight <= 0 || w <= 0 || h <= 0) {
            surfaceView.layout(0, 0, w, h)
            return
        }
        val videoAspect = videoWidth.toFloat() / videoHeight
        val (sw, sh) = if (w.toFloat() / h > videoAspect) {
            (h * videoAspect).toInt() to h
        } else {
            w to (w / videoAspect).toInt()
        }
        val l = (w - sw) / 2
        val t = (h - sh) / 2
        surfaceView.layout(l, t, l + sw, t + sh)
    }

    // ------------------------------------------------------------- session

    private fun startSession() {
        stopSession()
        val target = url ?: return
        val id = ++session
        everRendered = false
        connected = false
        emit(State.Connecting)

        val dec = LowLatencyH264Decoder(surfaceView.holder.surface, object : LowLatencyH264Decoder.Listener {
            override fun onVideoSize(width: Int, height: Int) = onMain(id) {
                videoWidth = width
                videoHeight = height
                requestLayout()
            }

            override fun onFirstFrame() = onMain(id) {
                everRendered = true
                failedSessionsWithoutVideo = 0
                reconnectDelay = RECONNECT_MIN_MS
                mainHandler.removeCallbacks(noFrameWatchdog)
                emit(State.Playing)
            }

            // Diagnostics go to logcat only: adb logcat -s ZeroCopyVideoView:V
            override fun onStats(stats: LowLatencyH264Decoder.Stats) {
                Log.v(TAG, "stats: $stats")
            }

            override fun onDecoderError(message: String, fatal: Boolean) = onMain(id) {
                if (fatal) fail(message) else retry(message)
            }
        })

        val ext = RtspNalExtractor(target, username, password, object : RtspNalExtractor.Listener {
            override fun onParameterSets(sps: ByteArray, pps: ByteArray) = dec.configure(sps, pps)

            override fun onAccessUnit(data: ByteArray, size: Int, isKeyFrame: Boolean) =
                dec.decode(data, size, isKeyFrame)

            override fun onConnected() = onMain(id) { connected = true }

            override fun onUnsupported(message: String) = onMain(id) { fail(message) }

            override fun onError(message: String) = onMain(id) { retry(message) }
        })

        decoder = dec
        extractor = ext
        mainHandler.postDelayed(noFrameWatchdog, NO_FRAME_TIMEOUT_MS)
        ext.start()
    }

    private fun stopSession() {
        session++
        mainHandler.removeCallbacksAndMessages(null)
        extractor?.stop()
        extractor = null
        decoder?.release()
        decoder = null
    }

    private val noFrameWatchdog = Runnable {
        if (!everRendered) retry("No video for ${NO_FRAME_TIMEOUT_MS / 1000}s")
    }

    private fun retry(reason: String) {
        if (url == null || fatal) return
        // Only sessions that reached PLAY but never showed a picture hint at an
        // incompatibility; plain network errors are retried forever.
        if (connected && !everRendered) failedSessionsWithoutVideo++
        if (failedSessionsWithoutVideo >= MAX_FAILED_SESSIONS_WITHOUT_VIDEO) {
            fail("Zero-copy path failed $failedSessionsWithoutVideo times: $reason")
            return
        }
        Log.w(TAG, "Reconnecting in ${reconnectDelay}ms: $reason")
        stopSession()
        emit(State.Reconnecting(reason))
        val delay = reconnectDelay
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_MAX_MS)
        mainHandler.postDelayed({ if (surfaceReady && url != null) startSession() }, delay)
    }

    private fun fail(reason: String) {
        if (fatal) return
        Log.e(TAG, "Fatal: $reason")
        fatal = true
        stopSession()
        emit(State.Fatal(reason))
    }

    private fun emit(state: State) {
        listener?.onState(state)
    }

    /** Runs [block] on the main thread only if [id] is still the active session. */
    private fun onMain(id: Int, block: () -> Unit) {
        mainHandler.post { if (id == session) block() }
    }
}
