package it.mobile.bisax.ptzvision.ui.console.blocks

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SecondaryCams(
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(0.333f)
                .background(Color(0xFFFF6666))
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color(0xFF6666FF))
                .weight(0.333f)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.334f)
                .background(Color(0xFF66FF66))
                .clickable {
                    Log.d("MainScreenLandscape", "Change Camera")
                }
        )
    }
}