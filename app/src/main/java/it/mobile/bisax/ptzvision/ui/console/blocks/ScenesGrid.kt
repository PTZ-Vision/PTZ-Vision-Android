package it.mobile.bisax.ptzvision.ui.console.blocks

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

data class ButtonData(
    val label: String,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
)

@Composable
fun ScenesGrid(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom
) {
    val buttons = List(9) {
        ButtonData(
            "Scene ${it+1}",
            { Log.d("Scene", "Scene ${it+1} click") },
            { Log.d("Scene", "Scene ${it+1} long click") },
        )
    }
    Column (modifier= modifier
        .fillMaxSize()
        .padding(
            0.dp,
            0.dp,
            0.dp,
            if (LocalConfiguration.current.screenWidthDp < 1000) 0.dp else 40.dp
        ),
        verticalArrangement = verticalArrangement
    ) {
        // 3 rows of 3 buttons
        for (i in 0 until 3) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 5.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    Text(
                        text = buttons[index].label,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { buttons[index].onLongClick() },
                                    onTap = { buttons[index].onClick() },
                                    onDoubleTap = { Log.d("Scene", "Scene $index double click") },
                                )
                            }
                            .background(Color.Red, CircleShape)
                            .padding(20.dp, 5.dp)
                    )
                }
            }
        }
    }
}