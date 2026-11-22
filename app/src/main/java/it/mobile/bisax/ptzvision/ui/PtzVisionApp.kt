package it.mobile.bisax.ptzvision.ui

import androidx.annotation.StringRes
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamProtocol
import it.mobile.bisax.ptzvision.ndi.NdiSource
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import it.mobile.bisax.ptzvision.ui.console.screen.MainScreen
import it.mobile.bisax.ptzvision.ui.home.HomeScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsScreen
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import it.mobile.bisax.ptzvision.ui.settings.cameraadd.CameraMode
import it.mobile.bisax.ptzvision.ui.settings.cameraadd.CameraSet
import it.mobile.bisax.ptzvision.ui.settings.ndi.NdiDiscoveryScreen

enum class PTZRoutes(@StringRes val route: Int) {
    HOME(R.string.home_route),
    SETTINGS(R.string.settings_route),
    CONSOLE(R.string.console_route),
    CAMERA_ADD(R.string.camera_add_route),
    CAMERA_EDIT(R.string.camera_edit_route),
    NDI_DISCOVERY(R.string.ndi_discovery_route)
}

@Composable
fun PtzVisionApp(
    navController: NavHostController = rememberNavController(),
    windowSize: WindowSizeClass,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel
) {
    val currentRoute = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute.value = destination.route
            if (destination.route != PTZRoutes.CONSOLE.name && mainViewModel.uiState.value.ptzController != null) {
                mainViewModel.resetPTZController()
            }
        }
    }


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
                    },
                    onNdiDiscoveryClick = {
                        if (navController.currentDestination?.route != PTZRoutes.NDI_DISCOVERY.name)
                            navController.navigate(PTZRoutes.NDI_DISCOVERY.name)
                    }
                )
            }

            composable(route = PTZRoutes.NDI_DISCOVERY.name) {
                val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
                NdiDiscoveryScreen(
                    existingCams = settingsUiState.cams,
                    onBack = { navController.popBackStack() },
                    onAddSource = { navToCameraAddNdi(navController, it) }
                )
            }

            composable(
                route = PTZRoutes.CAMERA_ADD.name + "/ndi?source={source}&name={name}&ip={ip}",
                arguments = listOf("source", "name", "ip").map { arg ->
                    navArgument(arg) { type = NavType.StringType; defaultValue = "" }
                }
            ) {
                CameraSet(
                    settingsViewModel = settingsViewModel,
                    context = LocalContext.current,
                    onBack = {
                        // Back to the settings list (skipping the discovery screen).
                        navController.popBackStack(PTZRoutes.SETTINGS.name, inclusive = false)
                    },
                    mode = CameraMode.ADD,
                    camName = it.arguments?.getString("name")?.ifEmpty { null } ?: "NDI Camera",
                    camIp = it.arguments?.getString("ip") ?: "",
                    camProtocol = CamProtocol.NDI,
                    camNdiSourceName = it.arguments?.getString("source") ?: ""
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
                route = PTZRoutes.CAMERA_EDIT.name + "/{camId}"
            ) {
                val camId = it.arguments?.getString("camId")?.toIntOrNull()
                val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
                val cam = settingsUiState.cams.firstOrNull { c -> c.id == camId }
                if (cam != null) {
                    CameraSet(
                        settingsViewModel = settingsViewModel,
                        context = LocalContext.current,
                        onBack = {
                            navToGeneral(navController = navController)
                        },
                        mode = CameraMode.MODIFY,
                        camId = cam.id,
                        camName = cam.name,
                        camIp = cam.ip,
                        camControlPort = cam.controlPort,
                        camStreamPort = cam.streamPort,
                        camHttpPort = cam.httpPort,
                        camActive = cam.active,
                        camProtocol = cam.protocol,
                        camNdiSourceName = cam.ndiSourceName
                    )
                }
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
    if (navController.currentDestination?.route != PTZRoutes.CONSOLE.name)
        navController.navigate(PTZRoutes.CONSOLE.name)
}

fun navToGeneral(
    navController: NavHostController
){
    if (navController.currentDestination?.route != PTZRoutes.HOME.name)
        navController.popBackStack()
}

fun navToSettings(
    navController: NavHostController
){
    if(navController.currentDestination?.route != PTZRoutes.SETTINGS.name)
        navController.navigate(PTZRoutes.SETTINGS.name)
}

fun navToCameraAdd(
    navController: NavHostController
){
    if (navController.currentDestination?.route != PTZRoutes.CAMERA_ADD.name)
        navController.navigate(PTZRoutes.CAMERA_ADD.name)
}

fun navToCameraEdit(
    navController: NavHostController,
    cam: Cam
){
    if (navController.currentDestination?.route != PTZRoutes.CAMERA_EDIT.name)
        navController.navigate(PTZRoutes.CAMERA_EDIT.name + "/${cam.id}")
}

fun navToCameraAddNdi(
    navController: NavHostController,
    source: NdiSource
) {
    val route = PTZRoutes.CAMERA_ADD.name + "/ndi" +
        "?source=" + Uri.encode(source.fullName) +
        "&name=" + Uri.encode(source.sourceName) +
        "&ip=" + Uri.encode(source.host ?: "")
    navController.navigate(route)
}