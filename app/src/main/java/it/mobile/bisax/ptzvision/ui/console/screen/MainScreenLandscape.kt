package it.mobile.bisax.ptzvision.ui.console.screen

import android.content.Context
import android.os.Build
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
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.blocks.JoyStick
import it.mobile.bisax.ptzvision.ui.console.blocks.ScenesGrid
import it.mobile.bisax.ptzvision.ui.console.blocks.SecondaryCams
import it.mobile.bisax.ptzvision.ui.console.blocks.SelectedCam
import it.mobile.bisax.ptzvision.ui.console.blocks.SliderBox
import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreenLandscape(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    windowSize: WindowSizeClass,
    context: Context
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()

    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .weight(0.5f)
        ) {
            if(settingsUiState.layout == SettingsUiState.Layout.J_LEFT){
                SelectedCam(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    cam = mainUiState.activeCams.getOrNull(0)
                )
            }
            Column(modifier = Modifier
                .weight(0.5f)
            ) {
                SecondaryCams(
                    modifier = Modifier
                        .weight(0.7f),
                    mainViewModel = mainViewModel,
                    context = context
                )
                Row (modifier= Modifier
                    .fillMaxWidth()
                    .weight(0.3f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val aiBtn = @Composable {
                        Button(
                            onClick = { mainViewModel.toggleAI() },
                            enabled =  mainUiState.activeCams.isNotEmpty(),
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
                            colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAIEnabled) Color.Green else Color.Gray)
                        ) {
                            Text(text = "AI Tracking")
                        }
                    }

                    if(settingsUiState.layout == SettingsUiState.Layout.J_LEFT)
                        aiBtn()

                    Row(modifier = Modifier
                        .weight(0.6f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10.5x",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = if(windowSize.widthSizeClass == WindowWidthSizeClass.Compact) 15.sp else 20.sp
                        )
                        Button(
                            onClick = { mainViewModel.toggleAutoFocus() },
                            modifier = Modifier
                                .weight(0.5f)
                                .then(
                                    if (windowSize.widthSizeClass <= WindowWidthSizeClass.Medium) {
                                        Modifier.padding(15.dp, 5.dp)
                                    } else {
                                        Modifier.padding(24.dp, 0.dp)
                                    }
                                ),
                            enabled = !(mainUiState.isAIEnabled) && mainUiState.activeCams.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAutoFocusEnabled) Color.Green else Color.Gray)
                        ) {
                            Text(
                                text = "Auto",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if(settingsUiState.layout == SettingsUiState.Layout.J_RIGHT)
                        aiBtn()
                }
            }
            if(settingsUiState.layout == SettingsUiState.Layout.J_RIGHT) {
                SelectedCam(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    cam = mainUiState.activeCams.getOrNull(0)
                )
            }
        }
        Row(modifier = Modifier
            .fillMaxSize()
            .weight(0.5f)) {

            val sliders = @Composable{
                SliderBox(
                    modifier = Modifier
                        .weight(0.15f)
                        .padding(15.dp),
                    setPosition = { maxPos, posY -> mainViewModel.setZoomIntensity(maxPos, posY) },
                    enabled = !(mainUiState.isAIEnabled) && mainUiState.activeCams.isNotEmpty(),
                    hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled
                )
                SliderBox(
                    modifier = Modifier
                        .weight(0.15f)
                        .padding(15.dp),
                    setPosition = { maxPos, posY -> mainViewModel.setFocusIntensity(maxPos,posY) },
                    enabled = !(mainUiState.isAutoFocusEnabled || mainUiState.isAIEnabled) && mainUiState.activeCams.isNotEmpty(),
                    hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled
                )
            }

            val joystick = @Composable{
                JoyStick(
                    modifier = Modifier
                        .weight(0.3f),
                    enabled = !(mainUiState.isAIEnabled) && mainUiState.activeCams.isNotEmpty(),
                    mainViewModel = mainViewModel,
                    hapticFeedbackEnabled = settingsUiState.hapticFeedbackEnabled
                )
            }

            if(settingsUiState.layout == SettingsUiState.Layout.J_LEFT){
                joystick()
            }
            else{
                sliders()
            }

            ScenesGrid(
                modifier = Modifier.weight(0.4f),
                windowSize = windowSize,
                isLandScape = true,
                mainViewModel = mainViewModel,
                enabled = mainUiState.activeCams.isNotEmpty()
            )

            if(settingsUiState.layout == SettingsUiState.Layout.J_RIGHT) {
                joystick()
            }
            else{
                sliders()
            }
        }
    }

}