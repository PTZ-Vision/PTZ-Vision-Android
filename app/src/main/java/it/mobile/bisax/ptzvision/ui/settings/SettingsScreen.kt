package it.mobile.bisax.ptzvision.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.settings.sections.Camera
import it.mobile.bisax.ptzvision.ui.settings.sections.CameraAddBtn
import it.mobile.bisax.ptzvision.ui.settings.sections.Layout

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun SettingsScreen(
    context: Context,
    settingsViewModel: SettingsViewModel,
    goHome: () -> Unit,
    onCameraAddClick: () -> Unit,
    onCameraModifyClick: (Cam) -> Unit
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    val uiState by settingsViewModel.settingsUiState.collectAsState()

    Surface {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            item{
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = goHome
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go Back",
                            modifier = Modifier
                        )
                    }
                    Text(
                        text = "Settings",
                        modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                        style = TextStyle(fontSize = 24.sp)
                    )
                }
            }

            item{
                SettingsSection(
                    title = "Cameras",
                    helper = {
                        CameraAddBtn(
                            onAddClick = onCameraAddClick
                        )
                    }
                ){}
            }

            items(uiState.cams){ cam ->
                Camera(
                    context = context,
                    settingsViewModel,
                    cam,
                    onCameraModifyClick
                )
            }

            item{
                SettingsSection(title = "Layout"){
                    Layout(
                        settingsViewModel = settingsViewModel
                    )
                }
            }

            item{
                SettingsSection(
                    title = "Controller"
                ){
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Haptic Feedback (Beta)",
                            style = TextStyle(fontSize = 16.sp)
                        )
                        Switch(
                            checked = uiState.hapticFeedbackEnabled,
                            onCheckedChange = {
                                settingsViewModel.toggleHapticFeedback()
                            }
                        )
                    }
                }
            }
        }
    }
}