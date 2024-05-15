package it.mobile.bisax.ptzvision.ui.console.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel

data class ButtonData(
    val label: String,
    val onClick: (Offset) -> Unit,
    val onLongClick: (Offset) -> Unit,
)

@Composable
fun ScenesGrid(
    modifier: Modifier = Modifier,
    windowSize: WindowSizeClass,
    isLandScape: Boolean,
    mainViewModel: MainViewModel
) {
    val mainUiState by mainViewModel.uiState.collectAsState()

    val buttons: MutableMap<Int, ButtonData> = mutableMapOf()

    for (i in 0 until 9) {
        val scene = mainUiState.camScenes.find { it.slot == i }
        if (scene == null) {
            buttons[i] = ButtonData(
                label = "Empty",
                onClick = { },
                onLongClick = { mainViewModel.addScene(i, mainUiState.activeCams.getOrNull(mainUiState.selectedCamSlot)?.id ?: 0) }
            )
            continue
        }
        else {
            buttons[i] = ButtonData(
                label = scene.name,
                onClick = {
                    mainViewModel.sendSceneToDevice(scene)
                },
                onLongClick = {
                    mainViewModel.updateScene(
                        scene.id,
                        scene.slot,
                        "UP ${scene.slot}",
                        mainUiState.activeCams[mainUiState.selectedCamSlot]?.id ?: 0
                    )
                }
            )
        }
    }

    Column (modifier= modifier
        .fillMaxSize()
        .padding(
            0.dp,
            if (!isLandScape || windowSize.heightSizeClass <= WindowHeightSizeClass.Compact)
                0.dp
            else
                40.dp
        ),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // 3 rows of 3 buttons
        for (i in 0 until 3) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 5.dp)
                    .weight(0.8f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    Modifier
                        .width(0.dp)
                        .weight(0.1f))
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    val buttonData = buttons[index]!!
                    val onClick = rememberUpdatedState(buttonData.onClick)
                    val onLongClick = rememberUpdatedState(buttonData.onLongClick)
                    Text(
                        text = buttons[index]!!.label,

                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset -> onLongClick.value(offset) },
                                    onTap = { offset -> onClick.value(offset) }
                                )
                            }
                            .background(if(buttons[index]!!.label != "Empty") Color.Blue else Color.Gray, CircleShape)
                            .weight(0.8f)
                            .padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(
                        Modifier
                            .width(0.dp)
                            .weight(0.1f))
                }
            }
        }
    }
}