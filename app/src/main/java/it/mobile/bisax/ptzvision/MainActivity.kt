package it.mobile.bisax.ptzvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.ViewModelProvider
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import it.mobile.bisax.ptzvision.data.cam.CamsViewModelFactory
import it.mobile.bisax.ptzvision.ui.PtzVisionApp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.MainViewModelFactory
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModelFactory
import it.mobile.bisax.ptzvision.ui.theme.PTZVisionTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PTZVisionTheme {
                val windowSize = calculateWindowSizeClass(this)

                val camsFactory = CamsViewModelFactory(this)
                val camsViewModel = ViewModelProvider(this, camsFactory)[CamsViewModel::class.java]

                val mainFactory = MainViewModelFactory(camsViewModel)
                val mainViewModel = ViewModelProvider(this, mainFactory)[MainViewModel::class.java]

                val settingsFactory = SettingsViewModelFactory(this, camsViewModel)
                val settingsViewModel = ViewModelProvider(this, settingsFactory)[SettingsViewModel::class.java]

                PtzVisionApp(
                    windowSize = windowSize,
                    settingsViewModel = settingsViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}