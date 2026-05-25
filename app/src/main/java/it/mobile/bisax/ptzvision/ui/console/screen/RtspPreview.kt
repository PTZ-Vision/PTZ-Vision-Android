package it.mobile.bisax.ptzvision.ui.console.screen

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.mobile.bisax.ptzvision.BuildConfig
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.RtspClientAdapter
import it.mobile.bisax.ptzvision.ui.console.RtspStreamManager
import it.mobile.bisax.ptzvision.ui.console.rtspUrl
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency RTSP preview composable.
 *
 * It attempts the zero-buffer RtspClientAdapter first (Tier 1). If the adapter
 * fails, it falls back to RtspStreamManager (Tier 2, tuned ExoPlayer).
 */
@Composable
fun RtspPreview(
    cam: Cam,
    modifier: Modifier = Modifier,
    restartKey: Int = 0,
    onStateChanged: ((RtspStreamManager.StreamState) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val streamManager = remember { RtspStreamManager(context) }
    val rtspAdapter = remember { RtspClientAdapter(context) }
    val streamState by streamManager.state.collectAsState()

    LaunchedEffect(streamState) {
        onStateChanged?.invoke(streamState)
    }

    DisposableEffect(cam.id, restartKey) {
        onDispose {
            rtspAdapter.stop()
            streamManager.stop()
        }
    }

    Box(modifier = modifier) {
        key(cam.id, restartKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceView(ctx).also { sv ->
                        sv.keepScreenOn = true
                        val usingAdapter = AtomicBoolean(false)
                        val callback = object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                val url = cam.rtspUrl()
                                rtspAdapter.stop()
                                streamManager.stop()
                                usingAdapter.set(true)

                                rtspAdapter.start(
                                    rtspUrl = url,
                                    surfaceView = sv,
                                    onError = { _ ->
                                        if (usingAdapter.getAndSet(false)) {
                                            sv.post {
                                                rtspAdapter.stop()
                                                streamManager.start(url, sv)
                                            }
                                        }
                                    }
                                )
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                // Aspect ratio handled by the player.
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                usingAdapter.set(false)
                                rtspAdapter.stop()
                                streamManager.stop()
                            }
                        }
                        sv.holder.addCallback(callback)
                    }
                }
            )
        }

        when (streamState) {
            is RtspStreamManager.StreamState.Connecting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is RtspStreamManager.StreamState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Stream error - reconnecting...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> Unit
        }

        if (BuildConfig.DEBUG) {
            Text(
                text = if (streamState is RtspStreamManager.StreamState.Playing) "LIVE" else "",
                color = Color.Green,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
