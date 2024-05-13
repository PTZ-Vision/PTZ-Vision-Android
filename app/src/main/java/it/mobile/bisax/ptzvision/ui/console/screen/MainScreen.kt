package it.mobile.bisax.ptzvision.ui.console.screen

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    context: Context,
    windowSize: WindowSizeClass,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if(isLandscape){
        MainScreenLandscape(
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel,
            windowSize = windowSize,
            context = context
        )
    }
    else{
        MainScreenPortrait(
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel,
            windowSize = windowSize,
            context = context
        )
    }
}