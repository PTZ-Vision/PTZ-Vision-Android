package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.rtspUrl

/**
 * ZeroCopyGLSurfaceView
 *
 * Wires together:
 *   ZeroCopyRenderer
 *   ZeroCopyMediaCodecDecoder
 *   RtspNalExtractor
 */
class ZeroCopyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    companion object {
        private const val TAG = "ZeroCopyGLSurfaceView"
        private const val DEFAULT_WIDTH = 1920
        private const val DEFAULT_HEIGHT = 1080
    }

    private val renderer = ZeroCopyRenderer(
        onSurfaceReady = ::onDecoderSurfaceReady,
        onFrameAvailable = { requestRender() }
    )

    private var decoder: ZeroCopyMediaCodecDecoder? = null
    private var rtspExtractor: RtspNalExtractor? = null
    private var pendingCam: Cam? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var surfaceReady = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun startStream(cam: Cam, onError: ((String) -> Unit)? = null) {
        stopStream(clearPending = false)
        pendingCam = cam
        errorCallback = onError
        if (surfaceReady) {
            startExtractor(cam)
        }
    }

    fun stopStream(clearPending: Boolean = true) {
        stopExtractor()
        decoder?.stop()
        decoder = null
        if (clearPending) {
            pendingCam = null
            errorCallback = null
        }
    }

    override fun onPause() {
        super.onPause()
        stopExtractor()
    }

    override fun onResume() {
        super.onResume()
        pendingCam?.let { cam ->
            if (surfaceReady) startExtractor(cam)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopStream()
        renderer.release()
    }

    private fun onDecoderSurfaceReady(surface: Surface) {
        Log.d(TAG, "Decoder surface ready - configuring MediaCodec")
        surfaceReady = true
        decoder?.stop()
        decoder = ZeroCopyMediaCodecDecoder(surface).also {
            it.start(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        }
        pendingCam?.let { startExtractor(it) }
    }

    private fun startExtractor(cam: Cam) {
        stopExtractor()
        rtspExtractor = RtspNalExtractor(
            url = cam.rtspUrl(),
            username = "",
            password = "",
            onNalUnit = { data, pts -> decoder?.feedNalUnit(data, pts) },
            onError = { message ->
                post { errorCallback?.invoke(message) }
                stopExtractor()
            }
        ).also { it.start() }
    }

    private fun stopExtractor() {
        rtspExtractor?.stop()
        rtspExtractor = null
    }
}

/**
 * ZeroCopyVideoPreview - drop-in Composable replacement for the original
 * RTSP preview in PTZ-Vision.
 */
@Composable
fun ZeroCopyVideoPreview(
    cam: Cam?,
    modifier: Modifier = Modifier,
    restartKey: Int = 0,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val glView = remember(cam?.id, restartKey) {
        ZeroCopyGLSurfaceView(context)
    }

    DisposableEffect(cam?.id, restartKey) {
        cam?.let { glView.startStream(it, onError) }
        onDispose { glView.stopStream() }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier,
        update = { /* handled by remember keys */ }
    )
}
