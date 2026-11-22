package it.mobile.bisax.ptzvision.ui.console.blocks

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import it.mobile.bisax.ptzvision.ui.console.MainUiState
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.launch

@Composable
internal fun TrackingButton(
    modifier: Modifier,
    state: MainUiState,
    viewModel: MainViewModel,
    onClickFeedback: () -> Unit,
) {
    val controller = state.ptzController
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var open by remember(controller) { mutableStateOf(false) }
    var pending by remember(controller) { mutableStateOf(false) }
    val ndi = state.activeCams.firstOrNull()?.isNdi == true

    fun request(enabled: Boolean) {
        open = false
        pending = true
        scope.launch {
            try {
                if (!viewModel.setAITracking(enabled)) {
                    val message = if (ndi) "Tracking not confirmed. Check camera compatibility and the VISCA control port."
                        else "Camera did not confirm AI Tracking."
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            } finally {
                pending = false
            }
        }
    }

    Box(modifier) {
        Button(
            onClick = {
                onClickFeedback()
                // NDI cannot report an external mode change. Both commands remain available.
                if (ndi) open = true else request(!state.isAIEnabled)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = controller != null && !pending,
            colors = ButtonDefaults.buttonColors(containerColor = if (state.isAIEnabled) MaterialTheme.colorScheme.primary else Color.Gray),
        ) {
            Text("AI Tracking")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Enable tracking") }, onClick = { request(true) })
            DropdownMenuItem(text = { Text("Disable tracking") }, onClick = { request(false) })
        }
    }
}
