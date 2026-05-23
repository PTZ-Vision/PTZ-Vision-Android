package it.mobile.bisax.ptzvision.ui.console.blocks

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.zerocopy.ZeroCopyVideoPreview

private enum class StreamTier {
    PRIMARY,
    FALLBACK
}

private enum class StreamStatus {
    LOADING,
    PLAYING,
    ERROR
}

@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cam: Cam? = null,
    mainViewModel: MainViewModel
) {
    var streamTier by remember(cam) { mutableStateOf(StreamTier.PRIMARY) }
    var streamStatus by remember(cam) { mutableStateOf(StreamStatus.LOADING) }
    var restartToken by remember(cam) { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    val rtspUrl = cam?.let { "rtsp://${it.ip}:${it.streamPort}/2" }

    fun restartStream() {
        streamTier = StreamTier.PRIMARY
        streamStatus = StreamStatus.LOADING
        restartToken += 1
    }

    fun handleTierError(error: Throwable) {
        if (streamTier == StreamTier.PRIMARY) {
            Log.w(TAG, "PRIMARY tier failed, falling back to FALLBACK", error)
            streamTier = StreamTier.FALLBACK
            streamStatus = StreamStatus.LOADING
        } else {
            Log.w(TAG, "FALLBACK tier failed, setting ERROR state", error)
            streamStatus = StreamStatus.ERROR
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isPaused = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    isPaused = false
                    if (cam != null) {
                        restartStream()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    isPaused = true
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(cam) {
        if (cam != null && !isPaused) {
            restartStream()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (cam != null) {
            when {
                isPaused -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Streaming paused", color = Color.White)
                        ReconnectButton {
                            restartStream()
                            mainViewModel.resetPTZController()
                            mainViewModel.initPTZController()
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rtspUrl != null) {
                                key(restartToken, streamTier) {
                                    if (streamTier == StreamTier.PRIMARY) {
                                        ZeroCopyVideoPreview(
                                            rtspUrl = rtspUrl,
                                            modifier = Modifier.fillMaxSize(),
                                            onFirstFrame = {
                                                streamStatus = StreamStatus.PLAYING
                                            },
                                            onError = ::handleTierError
                                        )
                                    } else {
                                        RtspSurfacePreview(
                                            rtspUrl = rtspUrl,
                                            modifier = Modifier.fillMaxSize(),
                                            onPlaying = {
                                                streamStatus = StreamStatus.PLAYING
                                            },
                                            onError = {
                                                streamStatus = StreamStatus.ERROR
                                            }
                                        )
                                    }
                                }
                            }
                            if (streamStatus == StreamStatus.ERROR) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "Error while streaming", color = Color.White)
                                    ReconnectButton {
                                        restartStream()
                                        mainViewModel.resetPTZController()
                                        mainViewModel.initPTZController()
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${cam.name} (${cam.ip})",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Text(text = "No camera selected", color = Color.White)
        }
    }
}

@Composable
fun ReconnectButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
        ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.refresh),
            contentDescription = "Reconnect",
            modifier = Modifier.padding(end = 3.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(text = "Reconnect", color = MaterialTheme.colorScheme.onPrimary)
    }
}

private const val TAG = "SelectedCam"
