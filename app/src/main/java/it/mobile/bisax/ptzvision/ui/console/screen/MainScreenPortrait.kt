package it.mobile.bisax.ptzvision.ui.console.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState

@Composable
fun MainScreenPortrait(
    mainViewModel: MainViewModel
) {
    val mainUiState by mainViewModel.uiState.collectAsState()

    val aiBtn = @Composable{modifier:Modifier ->
        Button(
            onClick = { mainViewModel.toggleAI() },
            modifier = modifier
                .padding(25.dp, 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAIEnabled) Color.Green else Color.Gray)
        ) {
            Text(text = "AI Tracking")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryCams(
            modifier = Modifier
                .weight(0.2f)
        )
        SelectedCam(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
                .padding(0.dp, 0.dp, 0.dp, 10.dp)
        )

        Row (modifier= Modifier
            .then(
                if (LocalConfiguration.current.screenWidthDp < 1000) {
                    Modifier.height(50.dp)
                } else {
                    Modifier.height(70.dp)
                }
            )
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(mainUiState.layout == SettingsUiState.Layout.J_LEFT)
                aiBtn(Modifier.weight(0.5f))

            Row(modifier = Modifier
                .weight(0.5f),
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
                        .padding(5.dp, 5.dp),
                    enabled = !(mainUiState.isAIEnabled),
                    colors = ButtonDefaults.buttonColors(containerColor = if(mainUiState.isAutoFocusEnabled) Color.Green else Color.Gray)
                ) {
                    Text(
                        text = "Auto",
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if(mainUiState.layout == SettingsUiState.Layout.J_RIGHT)
                aiBtn(Modifier.weight(0.5f))
        }

        Row(modifier = Modifier
            .fillMaxSize()
            .weight(0.25f)) {

            if(mainUiState.layout == SettingsUiState.Layout.J_LEFT)
                JoyStick(
                    modifier = Modifier
                        .weight(0.5f),
                    enabled = !(mainUiState.isAIEnabled),
                    mainViewModel = mainViewModel
                )

            SliderBox(
                modifier = Modifier
                    .weight(0.25f)
                    .padding(15.dp),
                setPosition = { maxPos, posY -> mainViewModel.setZoomIntensity(maxPos, posY) },
                enabled = !(mainUiState.isAIEnabled)
            )
            SliderBox(
                modifier = Modifier
                    .weight(0.25f)
                    .padding(15.dp),
                setPosition = { maxPos, posY -> mainViewModel.setFocusIntensity(maxPos,posY) },
                enabled = !(mainUiState.isAutoFocusEnabled || mainUiState.isAIEnabled)
            )

            if(mainUiState.layout == SettingsUiState.Layout.J_RIGHT)
                JoyStick(
                    modifier = Modifier
                        .weight(0.5f),
                    enabled = !(mainUiState.isAIEnabled),
                    mainViewModel = mainViewModel
                )
        }
        ScenesGrid(
            modifier = Modifier.weight(0.25f),
            verticalArrangement = Arrangement.SpaceEvenly
        )

    }
}