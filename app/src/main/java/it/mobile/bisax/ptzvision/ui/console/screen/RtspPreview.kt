package it.mobile.bisax.ptzvision.ui.console.screen

import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.alexvas.rtsp.widget.RtspSurfaceView
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.RtspClientAdapter
import it.mobile.bisax.ptzvision.ui.console.rtspUrl

/**
 * Fallback RTSP preview (rtsp-client-android), used when the zero-copy
 * pipeline reports a stream it cannot handle.
 */
@Composable
fun RtspPreview(
    cam: Cam,
    modifier: Modifier = Modifier,
    restartKey: Int = 0,
    onStateChanged: ((RtspClientAdapter.State) -> Unit)? = null
) {
    val adapter = remember { RtspClientAdapter() }
    val url = cam.rtspUrl()
    val state by adapter.state.collectAsState()

    LaunchedEffect(state) { onStateChanged?.invoke(state) }

    DisposableEffect(url, restartKey) {
        onDispose { adapter.stop() }
    }

    Box(modifier = modifier) {
        key(url, restartKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RtspSurfaceView(ctx).also { sv ->
                        sv.keepScreenOn = true
                        sv.holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                adapter.start(url, sv)
                            }

                            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) = Unit

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                adapter.stop()
                            }
                        })
                    }
                }
            )
        }

        when (state) {
            RtspClientAdapter.State.Connecting -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.White) }

            is RtspClientAdapter.State.Error -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Stream error - reconnecting...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> Unit
        }
    }
}
