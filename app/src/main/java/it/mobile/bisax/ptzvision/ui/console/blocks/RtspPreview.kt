package it.mobile.bisax.ptzvision.ui.console.blocks

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import it.mobile.bisax.ptzvision.ui.console.RtspStreamManager

@OptIn(UnstableApi::class)
@Composable
fun RtspSurfacePreview(
    rtspUrl: String,
    modifier: Modifier = Modifier,
    onPlaying: () -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onPlayingState = rememberUpdatedState(onPlaying)
    val onErrorState = rememberUpdatedState(onError)

    val streamManager = remember {
        RtspStreamManager(
            context = context,
            scope = scope,
            onPlaying = { onPlayingState.value() },
            onError = { error -> onErrorState.value(error) }
        )
    }

    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).also { view ->
                surfaceViewRef = view
            }
        },
        modifier = modifier,
        update = { view ->
            surfaceViewRef = view
        }
    )

    LaunchedEffect(rtspUrl, surfaceViewRef) {
        val view = surfaceViewRef
        if (view != null) {
            streamManager.start(rtspUrl, view)
        }
    }

    DisposableEffect(rtspUrl) {
        onDispose {
            streamManager.release()
        }
    }
}
