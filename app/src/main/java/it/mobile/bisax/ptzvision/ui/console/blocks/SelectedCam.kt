package it.mobile.bisax.ptzvision.ui.console.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import it.mobile.bisax.ptzvision.data.cam.Cam

@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    cam: Cam? = null
) {
    Box(
        modifier = modifier
            .background(Color(0xFF666666))
    ){
        Text(
            text = "${cam?.name} - ${cam?.ip} - ${cam?.port}",
            color = Color.Green
        )
    }
}