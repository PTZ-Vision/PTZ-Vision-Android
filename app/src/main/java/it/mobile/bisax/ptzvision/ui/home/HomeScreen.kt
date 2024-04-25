package it.mobile.bisax.ptzvision.ui.home

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import it.mobile.bisax.ptzvision.R

@Composable
fun HomeScreen(
    context: Context,
    goToConsole: () -> Unit,
    goToSettings: () -> Unit
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
    ) {
        Column(){
            Button(onClick = goToConsole) {
                Text(text = stringResource(id = R.string.console_btn))
            }
            Button(onClick = goToSettings) {
                Text(text = stringResource(R.string.settings_btn))
            }
        }
    }
}