package it.mobile.bisax.ptzvision.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mobile.bisax.ptzvision.ui.settings.sections.Camera
import it.mobile.bisax.ptzvision.ui.settings.sections.Layout

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun SettingsScreen(
    context: Context,
    settingsViewModel: SettingsViewModel,
    goHome: () -> Unit,
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    Surface {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            item{
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                        style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp)
                    )
                }
            }

            item{
                SettingsSection(title = "Cameras")
            }
            items(settingsViewModel.getCameraList()){ cam ->
                Camera(cam)
            }

            item{
                SettingsSection(title = "Layout")
            }
            item{
                Layout(settingsViewModel = settingsViewModel)
            }
        }
    }
}