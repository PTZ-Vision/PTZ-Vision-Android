package it.mobile.bisax.ptzvision.ui.console.screen

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun MainScreenLandscape(
    mainViewModel: MainViewModel
) {
    val mainUiState by mainViewModel.uiState.collectAsState()

    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .weight(0.5f)) {
            Column(modifier = Modifier
                .weight(0.5f)) {
                SecondaryCams(
                    modifier = Modifier
                        .weight(0.7f)
                )
                Row (modifier= Modifier
                    .then(
                        if (LocalConfiguration.current.screenWidthDp < 1000) {
                            Modifier.height(50.dp)
                        } else {
                            Modifier.height(70.dp)
                        }
                    )
                    .fillMaxWidth()
                    .weight(0.3f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            fontSize = if (LocalConfiguration.current.screenWidthDp < 1000) 15.sp else 20.sp
                        )
                        Button(
                            onClick = { mainViewModel.toggleAutoFocus() },
                            modifier = Modifier
                                .weight(0.5f)
                                .then(
                                    if (LocalConfiguration.current.screenWidthDp < 1000) {
                                        Modifier.padding(15.dp, 5.dp)
                                    } else {
                                        Modifier.padding(30.dp, 0.dp)
                                    }
                                ),
                            enabled = !(mainUiState.isAIEnabled),
                            colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAutoFocusEnabled) Color.Green else Color.Gray)
                        ) {
                            Text(
                                text = "Auto",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Button(
                        onClick = { mainViewModel.toggleAI() },
                        modifier = Modifier
                            .weight(0.4f)
                            .then(
                                if (LocalConfiguration.current.screenWidthDp < 800) {
                                    Modifier.padding(10.dp, 0.dp)
                                } else if (LocalConfiguration.current.screenWidthDp < 1000) {
                                    Modifier.padding(20.dp, 0.dp)
                                } else {
                                    Modifier.padding(60.dp, 0.dp)
                                }
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAIEnabled) Color.Green else Color.Gray)
                    ) {
                        Text(text = "AI Tracking")
                    }
                }
            }
            SelectedCam(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
            )
        }
        Row(modifier = Modifier
            .fillMaxSize()
            .weight(0.5f)) {


            SliderBox(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(15.dp),
                setPosition = { maxPos, posY -> mainViewModel.setZoomIntensity(maxPos, posY) },
                enabled = !(mainUiState.isAIEnabled)
            )
            SliderBox(
                modifier = Modifier
                    .weight(0.15f)
                    .padding(15.dp),
                setPosition = { maxPos, posY -> mainViewModel.setFocusIntensity(maxPos,posY) },
                enabled = !(mainUiState.isAutoFocusEnabled || mainUiState.isAIEnabled)
            )
            ScenesGrid(
                modifier = Modifier.weight(0.40f),
                verticalArrangement =
                    if(LocalConfiguration.current.screenHeightDp < 600)
                        Arrangement.SpaceEvenly
                    else
                        Arrangement.Bottom
                )

            JoyStick(
                modifier = Modifier
                    .weight(0.3f),
                enabled = !(mainUiState.isAIEnabled),
                mainViewModel = mainViewModel
            )
        }
    }

}