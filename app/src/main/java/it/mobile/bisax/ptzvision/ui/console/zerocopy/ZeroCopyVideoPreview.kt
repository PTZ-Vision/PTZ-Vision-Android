package it.mobile.bisax.ptzvision.ui.console.zerocopy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.rtspUrl

/**
 * Compose host for [ZeroCopyVideoView].
 *
 * [onError] is invoked only for failures the zero-copy path cannot recover
 * from, so the caller can switch to the library-based fallback player.
 */
@Composable
fun ZeroCopyVideoPreview(
    cam: Cam?,
    modifier: Modifier = Modifier,
    restartKey: Int = 0,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnError by rememberUpdatedState(onError)
    // Key on the stream address only: Cam also carries PTZ state (pan/tilt/zoom)
    // and must not restart the stream every time the camera moves.
    val url = cam?.rtspUrl()
    var state by remember(url, restartKey) {
        mutableStateOf<ZeroCopyVideoView.State>(ZeroCopyVideoView.State.Connecting)
    }

    val view = remember(url, restartKey) { ZeroCopyVideoView(context) }

    DisposableEffect(view) {
        view.listener = object : ZeroCopyVideoView.Listener {
            override fun onState(s: ZeroCopyVideoView.State) {
                state = s
                if (s is ZeroCopyVideoView.State.Fatal) currentOnError?.invoke(s.reason)
            }
        }
        url?.let { view.start(it) }
        onDispose {
            view.listener = null
            view.stop()
        }
    }

    Box(modifier = modifier) {
        // key(): a new view instance needs a new AndroidView (factory runs once).
        key(view) {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        }

        when (val s = state) {
            ZeroCopyVideoView.State.Connecting -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.White) }

            is ZeroCopyVideoView.State.Reconnecting -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        "Reconnecting… (${s.reason})",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            else -> Unit
        }
    }
}
