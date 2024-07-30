package it.mobile.bisax.ptzvision.ui.console.blocks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.R
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxHeight()
                    .clickable {
                        if (uiState.activeCams.getOrNull(i) != null) {
                            coroutineScope.launch {
                                mainViewModel.setNewActiveCam(i)
                            }
                        } else {
                            onClick()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if(uiState.activeCams.getOrNull(i) != null) {
                    Text(
                        text = uiState.activeCams.getOrNull(i)!!.name,
                        color = Color.Black
                    )
                }
                else{
                    Image(
                        painter = painterResource(
                            id = R.drawable.videocam_off
                        ),
                        contentDescription = "No Camera",
                        colorFilter = ColorFilter.tint(Color.Black),
                        modifier = Modifier
                            .size(50.dp)
                    )
                    Text(
                        text = "No camera",
                        color = Color.Black
                    )
                }
            }
        }
    }

}