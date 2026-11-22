package it.mobile.bisax.ptzvision.ui.console.ndi

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ndi.NdiReceiver
import it.mobile.bisax.ptzvision.ndi.NdiReceiverPool
import it.mobile.bisax.ptzvision.ndi.NdiReceiverStatus
import kotlinx.coroutines.delay

/** How long we wait for the first video frame before reporting an error (and falling back). */
private const val FIRST_FRAME_TIMEOUT_MS = 12_000L

/**
 * NDI® video preview. Frames are decoded by the NDI SDK and blitted by the native bridge
 * directly into the SurfaceView's buffer queue. Shares the receiver with the PTZ controller.
 */
@Composable
fun NdiVideoPreview(
    cam: Cam,
    modifier: Modifier = Modifier,
    restartKey: Int = 0,
    onError: () -> Unit,
) {
    val currentOnError by rememberUpdatedState(onError)
    var receiver by remember { mutableStateOf<NdiReceiver?>(null) }
    var status by remember { mutableStateOf(NdiReceiverStatus()) }

    DisposableEffect(cam.ndiSourceName, restartKey) {
        val lease = NdiReceiverPool.acquire(cam.ndiSourceName)
        receiver = lease
        status = NdiReceiverStatus()
        onDispose {
            lease?.setSurface(null)
            lease?.release()
            receiver = null
        }
    }

    LaunchedEffect(receiver) {
        val r = receiver
        if (r == null) {
            currentOnError()
            return@LaunchedEffect
        }
        val start = System.currentTimeMillis()
        while (true) {
            status = r.status()
            if (status.frames == 0 && System.currentTimeMillis() - start > FIRST_FRAME_TIMEOUT_MS) {
                currentOnError()
                return@LaunchedEffect
            }
            delay(if (status.frames == 0) 250 else 1000)
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        val r = receiver
        if (r != null) {
            val ratio = if (status.width > 0 && status.height > 0) status.width.toFloat() / status.height else 16f / 9f
            key(r) {
                AndroidView(
                    modifier = Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = true).fillMaxSize(),
                    factory = { ctx ->
                        SurfaceView(ctx).also { sv ->
                            sv.keepScreenOn = true
                            sv.holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) = r.setSurface(holder.surface)
                                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) = Unit
                                override fun surfaceDestroyed(holder: SurfaceHolder) = r.setSurface(null)
                            })
                        }
                    }
                )
            }
        }

        if (status.frames == 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = if (status.connected) "Waiting for NDI video…" else "Connecting to ${cam.ndiSourceName}…",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (status.frames > 0) {
            Text(
                text = buildString {
                    append("NDI ${status.width}×${status.height}")
                    if (status.ptzSupported) append(" · PTZ")
                },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
