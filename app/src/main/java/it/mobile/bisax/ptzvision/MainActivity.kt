package it.mobile.bisax.ptzvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import it.mobile.bisax.ptzvision.ui.PtzVisionApp
import it.mobile.bisax.ptzvision.ui.theme.PTZVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PTZVisionTheme {
                PtzVisionApp()
            }
        }
    }
}