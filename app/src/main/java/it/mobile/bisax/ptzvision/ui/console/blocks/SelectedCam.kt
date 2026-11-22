package it.mobile.bisax.ptzvision.ui.console.blocks

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.LowLatencyWifiLockEffect
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.RtspClientAdapter
import it.mobile.bisax.ptzvision.ui.console.screen.RtspPreview
import it.mobile.bisax.ptzvision.ndi.NdiRuntime
import it.mobile.bisax.ptzvision.ui.console.ndi.NdiVideoPreview
import it.mobile.bisax.ptzvision.ui.console.zerocopy.ZeroCopyVideoPreview

@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    cam: Cam? = null,
    mainViewModel: MainViewModel
) {
    var useFallback by remember(cam?.id) { mutableStateOf(false) }
    var rtspState by remember(cam?.id) { mutableStateOf<RtspClientAdapter.State?>(null) }
    var restartKey by remember(cam?.id) { mutableStateOf(0) }

    fun resetStream() {
        useFallback = false
        restartKey += 1
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (cam != null) {
            LowLatencyWifiLockEffect()
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val useNdiVideo = cam.isNdi && NdiRuntime.isAvailable
                    if (useNdiVideo && !useFallback) {
                        NdiVideoPreview(
                            cam = cam,
                            modifier = Modifier.fillMaxSize(),
                            restartKey = restartKey,
                            onError = { useFallback = true }
                        )
                    } else if (cam.isNdi && cam.ip.isBlank()) {
                        // NDI camera without an IP: no RTSP fallback possible.
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (NdiRuntime.isAvailable) "No NDI video from ${cam.ndiSourceName}"
                                else "NDI runtime not available",
                                color = Color.White
                            )
                            if (NdiRuntime.isAvailable) ReconnectButton { resetStream() }
                        }
                    } else if (!useFallback && !cam.isNdi) {
                        ZeroCopyVideoPreview(
                            cam = cam,
                            modifier = Modifier.fillMaxSize(),
                            restartKey = restartKey,
                            onError = { useFallback = true }
                        )
                    } else {
                        RtspPreview(
                            cam = cam,
                            modifier = Modifier.fillMaxSize(),
                            restartKey = restartKey,
                            onStateChanged = { rtspState = it }
                        )
                    }

                    if (useFallback && rtspState is RtspClientAdapter.State.Error) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Error while streaming", color = Color.White)
                            ReconnectButton {
                                resetStream()
                                mainViewModel.resetPTZController()
                                mainViewModel.initPTZController()
                            }
                        }
                    }
                }

                Text(
                    text = if (cam.isNdi) "${cam.name} (NDI: ${cam.ndiSourceName})" else "${cam.name} (${cam.ip})",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    textAlign = TextAlign.Center
                )
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
