package it.mobile.bisax.ptzvision.ui.console.screen

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.blocks.FocusSlider
import it.mobile.bisax.ptzvision.ui.console.blocks.JoyStick
import it.mobile.bisax.ptzvision.ui.console.blocks.ScenesGrid
import it.mobile.bisax.ptzvision.ui.console.blocks.SecondaryCams
import it.mobile.bisax.ptzvision.ui.console.blocks.SelectedCam
import it.mobile.bisax.ptzvision.ui.console.blocks.ZoomSlider
import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreenLandscape(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    windowSize: WindowSizeClass,
    context: Context,
    onClick: () -> Unit
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
    val coroutine = rememberCoroutineScope()

    val cameraEnabled = mainUiState.activeCams.isNotEmpty() && mainUiState.ptzController != null

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .weight(0.5f)
        ) {
            if (settingsUiState.layout == SettingsUiState.Layout.J_LEFT) {
                SelectedCam(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    context = context,
                    cam = mainUiState.activeCams.getOrNull(0),
                    lifecycleOwner = context as LifecycleOwner,
                    mainViewModel = mainViewModel
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.5f)
            ) {
                SecondaryCams(
                    modifier = Modifier
                        .weight(0.7f),
                    mainViewModel = mainViewModel,
                    onClick = onClick
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val vibe = context.getSystemService(Vibrator::class.java) as Vibrator

                    val aiBtn = @Composable {
                        Button(
                            onClick = {
                                coroutine.launch {
                                    mainViewModel.toggleAI()
                                }
                                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            },
                            enabled = cameraEnabled,
                            modifier = Modifier
                                .weight(0.4f)
                                .then(
                                    when (windowSize.widthSizeClass) {
                                        WindowWidthSizeClass.Compact -> {
                                            Modifier.padding(10.dp, 0.dp)
                                        }

                                        WindowWidthSizeClass.Medium -> {
                                            Modifier.padding(10.dp, 0.dp)
                                        }

                                        else -> {
                                            Modifier.padding(20.dp, 0.dp)
                                        }
                                    }
                                ),
                            colors = ButtonDefaults.buttonColors(containerColor = if (mainUiState.isAIEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                        ) {
                            Text(text = "AI Tracking")
                        }
                    }

                    if (settingsUiState.layout == SettingsUiState.Layout.J_LEFT)
                        aiBtn()

                    Row(
                        modifier = Modifier
                            .weight(0.6f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mainUiState.zoomLevel.toString() + "x",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact) 15.sp else 20.sp
                        )
                        Button(
                            onClick = {
                                coroutine.launch {
                                    mainViewModel.toggleAutoFocus()
                                }
                                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .then(
                                    if (windowSize.widthSizeClass <= WindowWidthSizeClass.Medium) {
                                        Modifier.padding(20.dp, 5.dp)
                                    } else {
                                        Modifier.padding(24.dp, 0.dp)
                                    }
                                ),
                            enabled = !(mainUiState.isAIEnabled) && cameraEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = if (mainUiState.isAutoFocusEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                        ) {
                            Text(
                                text = "AF",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (settingsUiState.layout == SettingsUiState.Layout.J_RIGHT)
                        aiBtn()
                }
            }
            if (settingsUiState.layout == SettingsUiState.Layout.J_RIGHT) {
                SelectedCam(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    context = context,
                    cam = mainUiState.activeCams.getOrNull(0),
                    lifecycleOwner = context as LifecycleOwner,
                    mainViewModel = mainViewModel
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.5f)
        ) {
            val joystick = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    JoyStick(
                        modifier = Modifier
                            .weight(0.85f)
                            .padding(top = 10.dp),
                        enabled = !(mainUiState.isAIEnabled) && cameraEnabled,
                        mainViewModel = mainViewModel,
                        hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled
                    )
                    Text(
                        text = "Pan & Tilt",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .weight(0.15f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }
            }

            val sliders = @Composable {
                Column(
                    modifier = Modifier.weight(0.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ZoomSlider(
                        mainViewModel = mainViewModel,
                        hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled,
                        enabled = !(mainUiState.isAIEnabled) && cameraEnabled,
                        modifier = Modifier
                            .weight(0.85f)
                            .padding(top = 10.dp),
                        controller = mainUiState.ptzController
                    )
                    Text(
                        text = "Zoom",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(0.15f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(0.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FocusSlider(
                        mainViewModel = mainViewModel,
                        hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled,
                        enabled = !(mainUiState.isAutoFocusEnabled || mainUiState.isAIEnabled) && cameraEnabled,
                        modifier = Modifier
                            .weight(0.85f)
                            .padding(top = 10.dp),
                    )
                    Text(
                        text = "Focus",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(0.15f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }
            }

            if (settingsUiState.layout == SettingsUiState.Layout.J_LEFT) {
                joystick(
                    Modifier.weight(0.3f)
                )
            } else {
                sliders()
            }

            ScenesGrid(
                modifier = Modifier
                    .weight(0.45f)
                    .padding(top = 10.dp),
                mainViewModel = mainViewModel,
                enabled = cameraEnabled
            )

            if (settingsUiState.layout == SettingsUiState.Layout.J_RIGHT) {
                joystick(Modifier.weight(0.25f))
            } else {
                sliders()
            }
        }
    }

}