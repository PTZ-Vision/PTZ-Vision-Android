package it.mobile.bisax.ptzvision.ui.console.blocks

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.launch


data class ButtonData(
    val label: String,
    val onClick: (Offset) -> Unit,
    val onLongClick: (Offset) -> Unit,
)

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ScenesGrid(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    enabled: Boolean
) {
    val boxes: MutableMap<Int, ButtonData> = mutableMapOf()
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val vibe = context.getSystemService(Vibrator::class.java) as Vibrator

    for (i in 0 until 9) {
        boxes[i] = ButtonData(
            label = if (i == 0) "Home" else "Scene $i",
            onClick = { _ ->
                coroutine.launch {
                    mainViewModel.goToScene(i)
                }

                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            },
            onLongClick = { _ ->
                coroutine.launch {
                    mainViewModel.saveSceneOnCam(i)
                }
                val toast = Toast.makeText(
                    context,
                    if (i == 0) "Home scene saved" else "Scene $i saved",
                    Toast.LENGTH_SHORT
                )
                toast.show()
                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0 until 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.33333f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    val boxData = boxes[index]!!
                    val onClick = rememberUpdatedState(boxData.onClick)
                    val onLongClick = rememberUpdatedState(boxData.onLongClick)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .then(
                                if (enabled)
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { offset -> onLongClick.value(offset) },
                                            onTap = { offset -> onClick.value(offset) }
                                        )
                                    }
                                else
                                    Modifier
                            )
                            .background(if (enabled) MaterialTheme.colorScheme.tertiaryContainer else Color.Gray)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surface
                            )
                            .weight(0.33333f)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = boxes[index]!!.label,
                            textAlign = TextAlign.Center,
                            color = if (enabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}