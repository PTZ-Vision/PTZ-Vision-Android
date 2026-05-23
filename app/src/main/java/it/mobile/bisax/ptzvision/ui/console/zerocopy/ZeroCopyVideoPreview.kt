package it.mobile.bisax.ptzvision.ui.console.zerocopy

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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ZeroCopyVideoPreview(
    rtspUrl: String,
    modifier: Modifier = Modifier,
    onFirstFrame: () -> Unit,
    onError: (Throwable) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val onFirstFrameState = rememberUpdatedState(onFirstFrame)
    val onErrorState = rememberUpdatedState(onError)

    val adapter = remember(rtspUrl) {
        RtspClientAdapter(
            scope = scope,
            onFirstFrame = { onFirstFrameState.value() },
            onError = { error -> onErrorState.value(error) }
        )
    }

    var viewRef by remember { mutableStateOf<ZeroCopyGLSurfaceView?>(null) }
    var surface by remember { mutableStateOf<Surface?>(null) }
    val isActive = remember { AtomicBoolean(true) }

    AndroidView(
        factory = { context ->
            ZeroCopyGLSurfaceView(context).also { view ->
                viewRef = view
                view.setOnSurfaceReadyListener { newSurface ->
                    if (isActive.get()) {
                        surface = newSurface
                    } else {
                        newSurface.release()
                    }
                }
            }
        },
        modifier = modifier,
        update = { view ->
            viewRef = view
        }
    )

    LaunchedEffect(rtspUrl, surface) {
        val readySurface = surface
        if (readySurface != null) {
            adapter.start(rtspUrl, readySurface)
        }
    }

    DisposableEffect(lifecycleOwner, viewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> viewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(rtspUrl) {
        isActive.set(true)
        onDispose {
            isActive.set(false)
            adapter.stop()
            surface?.release()
            surface = null
            viewRef?.release()
        }
    }
}
