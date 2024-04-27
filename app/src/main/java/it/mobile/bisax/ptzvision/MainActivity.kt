package it.mobile.bisax.ptzvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import it.mobile.bisax.ptzvision.ui.PtzVisionApp
import it.mobile.bisax.ptzvision.ui.theme.PTZVisionTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PTZVisionTheme {
                val windowSize = calculateWindowSizeClass(this)
                PtzVisionApp(
                    windowSize = windowSize
                )
            }
        }
    }
}