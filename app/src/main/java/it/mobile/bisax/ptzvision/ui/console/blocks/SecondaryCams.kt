package it.mobile.bisax.ptzvision.ui.console.blocks

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
    onClick: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Row(modifier = modifier) {
        for (i in 1..3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxHeight()
                    .clickable {
                        if (uiState.activeCams.getOrNull(i) != null) {
                            coroutineScope.launch {
                                mainViewModel.setNewActiveCam(i)
                            }
                        }
                        else {
                            onClick()
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