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
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.screen.MainScreen
import it.mobile.bisax.ptzvision.ui.home.HomeScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import it.mobile.bisax.ptzvision.ui.settings.cameraadd.CameraMode
import it.mobile.bisax.ptzvision.ui.settings.cameraadd.CameraSet

enum class PTZRoutes(@StringRes val route: Int) {
    HOME(R.string.home_route),
    SETTINGS(R.string.settings_route),
    CONSOLE(R.string.console_route),
    CAMERA_ADD(R.string.camera_add_route),
    CAMERA_EDIT(R.string.camera_edit_route)
}

@Composable
fun PtzVisionApp(
    navController: NavHostController = rememberNavController(),
    windowSize: WindowSizeClass,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel
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
                    settingsViewModel = settingsViewModel,
                    goHome = {
                        navToGeneral(navController = navController)
                    },
                    onCameraAddClick = {
                        navToCameraAdd(navController = navController)
                    },
                    onCameraModifyClick = {
                        navToCameraEdit(navController = navController, it)
                    }
                )
            }

            composable(
                route =  PTZRoutes.CAMERA_ADD.name
            ) {
                CameraSet(
                    settingsViewModel = settingsViewModel,
                    context = LocalContext.current,
                    onBack = {
                        navToGeneral(navController = navController)
                    },
                    mode = CameraMode.ADD
                )
            }

            composable(
                route =  PTZRoutes.CAMERA_EDIT.name + "/{camId}/{camName}/{camIp}/{camPort}/{camStreamPort}/{camActive}"
            ) {
                CameraSet (
                    settingsViewModel = settingsViewModel,
                    context = LocalContext.current,
                    onBack = {
                        navToGeneral(navController = navController)
                    },
                    mode = CameraMode.MODIFY,
                    camId = it.arguments?.getString("camId")?.toInt() ?: 0,
                    camName = it.arguments?.getString("camName") ?: "",
                    camIp = it.arguments?.getString("camIp") ?: "",
                    camPort = it.arguments?.getString("camPort")?.toInt() ?: 0,
                    camStreamPort = it.arguments?.getString("camStreamPort")?.toInt() ?: 0,
                    camActive = it.arguments?.getString("camActive")?.toBoolean() ?: false
                )
            }

            composable(route =  PTZRoutes.CONSOLE.name){
                MainScreen(
                    context = LocalContext.current,
                    windowSize = windowSize,
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    goToSettings = { navToSettings(navController) }
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

fun navToCameraAdd(
    navController: NavHostController
){
    navController.navigate(PTZRoutes.CAMERA_ADD.name)
}

fun navToCameraEdit(
    navController: NavHostController,
    cam: Cam
){
    navController.navigate(PTZRoutes.CAMERA_EDIT.name + "/${cam.id}/${cam.name}/${cam.ip}/${cam.port}/${cam.streamPort}/${cam.active}")
}