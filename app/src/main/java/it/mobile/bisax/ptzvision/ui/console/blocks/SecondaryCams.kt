package it.mobile.bisax.ptzvision.ui.console.blocks

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SecondaryCams(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    context: Context
) {
    val uiState by mainViewModel.uiState.collectAsState()
    var toast: Toast? = null
    val coroutineScope = rememberCoroutineScope()

    Row(modifier = modifier) {
        for (i in 0..3) {
            if(i != uiState.selectedCamSlot){
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary)
                        .fillMaxHeight()
                        .clickable {
                            if (uiState.activeCams.getOrNull(i) != null) {
                                coroutineScope.launch {
                                    mainViewModel.setNewActiveCam(i, uiState.activeCams[i]!!.id)
                                }
                            } else {
                                toast?.cancel()
                                toast = Toast.makeText(
                                    context,
                                    "No camera in slot $i",
                                    Toast.LENGTH_SHORT
                                )
                                toast?.show()
                            }
                        }
                ) {
                    Text(
                        text = uiState.activeCams.getOrNull(i)?.name ?: "NO SIGNAL",
                        color = Color.Red
                    )
                }
            }
        }
    }
}