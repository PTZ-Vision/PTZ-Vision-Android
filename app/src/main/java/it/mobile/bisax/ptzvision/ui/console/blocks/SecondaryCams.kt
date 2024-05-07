package it.mobile.bisax.ptzvision.ui.console.blocks

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SecondaryCams(
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(0.333f)
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.secondary)
                .weight(0.333f)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.334f)
                .background(MaterialTheme.colorScheme.tertiary)
                .clickable {
                    Log.d("MainScreenLandscape", "Change Camera")
                }
        )
    }
}