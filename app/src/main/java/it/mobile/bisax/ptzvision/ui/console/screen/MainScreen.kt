package it.mobile.bisax.ptzvision.ui.console.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    context: Context,
    windowSize: WindowSizeClass,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    goToSettings: () -> Unit
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutine = rememberCoroutineScope()

    val mainUiState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(mainUiState) {
        if(mainUiState.ptzController == null){
            mainViewModel.initPTZController()
        }
    }

    val onClick: () -> Unit = {
        coroutine.launch {
            val result = snackbarHostState
                .showSnackbar(
                    message = "No camera assigned",
                    actionLabel = "Go to Settings",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    goToSettings()
                }
                SnackbarResult.Dismissed -> {
                    /* Handle snackbar dismissed */
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier
            .fillMaxSize()
    ) { _ ->
        if (isLandscape) {
            MainScreenLandscape(
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                windowSize = windowSize,
                context = context,
                onClick = onClick
            )
        } else {
            MainScreenPortrait(
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                windowSize = windowSize,
                context = context,
                onClick = onClick
            )
        }
    }
}