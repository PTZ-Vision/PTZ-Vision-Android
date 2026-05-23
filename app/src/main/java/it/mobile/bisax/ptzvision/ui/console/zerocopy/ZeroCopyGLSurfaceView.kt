package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.opengl.GLSurfaceView

class ZeroCopyGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private var surfaceReadyListener: ((Surface) -> Unit)? = null
    private val renderer = ZeroCopyRenderer(
        onSurfaceReady = { surface -> surfaceReadyListener?.invoke(surface) },
        requestRender = { requestRender() }
    )

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setOnSurfaceReadyListener(listener: (Surface) -> Unit) {
        surfaceReadyListener = listener
    }

    fun release() {
        renderer.release()
    }
}
