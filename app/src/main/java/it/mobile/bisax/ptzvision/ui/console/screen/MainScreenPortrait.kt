package it.mobile.bisax.ptzvision.ui.console.screen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import it.mobile.bisax.ptzvision.ui.console.blocks.JoyStick
import it.mobile.bisax.ptzvision.ui.console.blocks.ScenesGrid
import it.mobile.bisax.ptzvision.ui.console.blocks.SecondaryCams
import it.mobile.bisax.ptzvision.ui.console.blocks.SelectedCam
import it.mobile.bisax.ptzvision.ui.console.blocks.SliderBox
import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreenPortrait(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    windowSize: WindowSizeClass,
    context: Context,
    onClick: () -> Unit,
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()

    val coroutine = rememberCoroutineScope()
    val cameraEnabled = mainUiState.activeCams.isNotEmpty() && mainUiState.ptzController != null


    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryCams(
            modifier = Modifier
                .weight(0.15f),
            mainViewModel = mainViewModel,
            onClick = onClick
        )
        SelectedCam(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .padding(0.dp, 0.dp, 0.dp, 10.dp),
            context = context,
            cam = mainUiState.activeCams.getOrNull(0),
            lifecycleOwner = context as LifecycleOwner,
            mainViewModel = mainViewModel
        )

        Row(
            modifier = Modifier
                .height(70.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val vibe = context.getSystemService(Vibrator::class.java) as Vibrator

            val aiBtn = @Composable { modifier: Modifier ->
                Button(
                    onClick = {
                        coroutine.launch {
                            mainViewModel.toggleAI()
                        }
                        vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    },
                    modifier = modifier
                        .padding(
                            if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact)
                                25.dp
                            else
                                60.dp,
                            0.dp
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mainUiState.isAIEnabled) MaterialTheme.colorScheme.primary else Color.Gray),
                    enabled = cameraEnabled
                ) {
                    Text(text = "AI Tracking")
                }
            }

            if (settingsUiState.layout == SettingsUiState.Layout.J_LEFT)
                aiBtn(Modifier.weight(0.5f))

            Row(
                modifier = Modifier
                    .weight(0.5f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mainUiState.zoomLevel.toString()+"x",
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
                        .padding(5.dp, 5.dp),
                    enabled = !(mainUiState.isAIEnabled) && cameraEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = if (mainUiState.isAutoFocusEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                ) {
                    Text(
                        text = "Auto",
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (settingsUiState.layout == SettingsUiState.Layout.J_RIGHT)
                aiBtn(Modifier.weight(0.5f))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.3f)
        ) {
            val joystickWithLabel = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    JoyStick(
                        modifier = Modifier
                            .weight(0.85f)
                            .padding(10.dp, 0.dp, 10.dp, 10.dp),
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

            val sliderWithLabel = @Composable { modifier: Modifier, label: String, enabled: Boolean, updateStatus: suspend () -> Unit, onDrag: suspend (maxPos: Float, posY: Float) -> Unit  ->
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SliderBox(
                        modifier = Modifier
                            .weight(0.85f)
                            .padding(10.dp, 0.dp, 10.dp, 10.dp),
                        setPosition = { maxPos, posY ->
                            coroutine.launch {
                                if(mainUiState.ptzController != null)
                                    onDrag(maxPos, posY)
                            }
                        },
                        enabled = enabled,
                        hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled,
                        updateStatus = updateStatus,
                    )
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .weight(0.15f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }
            }

            if (settingsUiState.layout == SettingsUiState.Layout.J_LEFT)
                joystickWithLabel(Modifier.weight(0.5f))

            sliderWithLabel(
                Modifier.weight(0.25f),
                "Zoom",
                !(mainUiState.isAIEnabled) && cameraEnabled,
                {
                    if(mainUiState.ptzController != null)
                        mainViewModel.updateZoomLevel()
                }
            ) { maxPos, posY ->
                mainViewModel.setZoomIntensity(maxPos, posY)
            }
            sliderWithLabel(
                Modifier.weight(0.25f),
                "Focus",
                !(mainUiState.isAutoFocusEnabled || mainUiState.isAIEnabled) && cameraEnabled,
                {}
            ) { maxPos, posY ->
                mainViewModel.setFocusIntensity(maxPos, posY)
            }

            if (settingsUiState.layout == SettingsUiState.Layout.J_RIGHT)
                joystickWithLabel(Modifier.weight(0.5f))

        }
        ScenesGrid(
            modifier = Modifier.weight(0.30f)
                .padding(top = 20.dp),
            mainViewModel = mainViewModel,
            enabled = cameraEnabled
        )
    }
}