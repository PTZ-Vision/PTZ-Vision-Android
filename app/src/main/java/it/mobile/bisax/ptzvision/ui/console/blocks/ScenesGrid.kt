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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.launch

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
    mainViewModel: MainViewModel,
    enabled: Boolean
) {
    val buttons: MutableMap<Int, ButtonData> = mutableMapOf()
    val coroutine = rememberCoroutineScope()

    for (i in 0 until 9) {
        buttons[i] = ButtonData(
            label = if(i == 0) "Home" else "Scene $i",
            onClick = { _ -> coroutine.launch {
                mainViewModel.goToScene(i)
            } },
            onLongClick = { _ ->
                coroutine.launch {
                    mainViewModel.saveSceneOnCam(i)
                }
            }
        )
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
                            .then(
                                if(enabled)
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { offset -> onLongClick.value(offset) },
                                            onTap = { offset -> onClick.value(offset) }
                                        )
                                    }
                                else
                                    Modifier
                            )
                            .background(if(enabled) Color.Blue else Color.Gray, CircleShape)
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