package it.mobile.bisax.ptzvision.ui

import androidx.annotation.StringRes
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.ui.console.screen.MainScreen
import it.mobile.bisax.ptzvision.ui.home.HomeScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel

enum class PTZRoutes(@StringRes val route: Int) {
    HOME(R.string.home_route),
    SETTINGS(R.string.settings_route),
    CONSOLE(R.string.console_route)
}

@Composable
fun PtzVisionApp(
    navController: NavHostController = rememberNavController(),
    windowSize: WindowSizeClass
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = PTZRoutes.HOME.name,
            modifier = Modifier
                .fillMaxSize(),
            enterTransition = {
                slideInHorizontally { initialOffsetX -> initialOffsetX}
            },
            exitTransition = {
                slideOutHorizontally { initialOffsetX -> -initialOffsetX*2 }
            }
        ){
            composable(route =  PTZRoutes.HOME.name){
                HomeScreen(
                    goToConsole = {
                        navToConsole(navController = navController)
                    },
                    goToSettings = {
                        navToSettings(navController = navController)
                    },
                    windowSize = windowSize
                )
            }

            composable(route =  PTZRoutes.SETTINGS.name){
                SettingsScreen(
                    context = LocalContext.current,
                    settingsViewModel = SettingsViewModel(LocalContext.current),
                    goHome = {
                        navToGeneral(navController = navController)
                    })
            }

            composable(route =  PTZRoutes.CONSOLE.name){
                MainScreen(
                    context = LocalContext.current,
                    windowSize = windowSize
                )
            }
        }
    }
}

fun navToConsole(
    navController: NavHostController
){
    navController.navigate(PTZRoutes.CONSOLE.name)
}

fun navToGeneral(
    navController: NavHostController
){
    navController.popBackStack()
}

fun navToSettings(
    navController: NavHostController
){
    navController.navigate(PTZRoutes.SETTINGS.name)
}

fun goBack(){

}