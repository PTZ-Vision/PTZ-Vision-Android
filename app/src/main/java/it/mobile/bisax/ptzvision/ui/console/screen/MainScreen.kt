package it.mobile.bisax.ptzvision.ui.console.screen

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import it.mobile.bisax.ptzvision.ui.console.MainViewModel

@Composable
fun MainScreen(
    context: Context
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val mainViewModel = MainViewModel(context = context)

    if(isLandscape){
        MainScreenLandscape(mainViewModel = mainViewModel)
    }
    else{
        MainScreenPortrait(mainViewModel = mainViewModel)
    }
}