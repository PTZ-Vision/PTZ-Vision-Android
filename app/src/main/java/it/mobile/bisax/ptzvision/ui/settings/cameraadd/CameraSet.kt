package it.mobile.bisax.ptzvision.ui.settings.cameraadd

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

enum class CameraMode {
    ADD,
    MODIFY
}

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun CameraSet(
    settingsViewModel: SettingsViewModel,
    context: Context,
    onBack: () -> Unit,
    mode: CameraMode,
    camId: Int = 0,
    camName: String = "New Camera",
    camIp: String = "",
    camPort: Int = 0,
    camStreamPort: Int = 0,
    camActive: Boolean = false
) {
    // lock orientation to vertical
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    Surface(
        modifier = Modifier.fillMaxSize(),
    ){
        var name by remember { mutableStateOf(camName) }
        var ip by remember { mutableStateOf(camIp) }
        var port by remember { mutableIntStateOf(camPort) }
        var streamPort by remember { mutableIntStateOf(camStreamPort) }
        var active by remember { mutableStateOf(camActive) }

        Column{
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Go Back",
                        modifier = Modifier
                    )
                }
                Text(
                    text = "Add a Camera",
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                    style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp)
                )
            }
            CameraInput(name = name) {
                name = it
            }
            IPAddressInput(ip = ip) {
                ip = it
            }
            PortInput(title="Control Port", port = port) {
                port = it
            }
            PortInput(title="Streaming Port", port = streamPort) {
                streamPort = it
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ){
                Switch(
                    checked = active,
                    onCheckedChange = { active = it }
                )
                Text(text = "Add to Console", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = {
                    setCamera(
                        settingsViewModel = settingsViewModel,
                        context = context,
                        onBack = onBack,
                        mode = mode,
                        id = camId,
                        name = name,
                        ip = ip,
                        port = port,
                        streamPort = streamPort,
                        active = active
                    )
                },
                modifier = Modifier.padding(start = 16.dp),
            ){
                Text(
                    text = if(mode == CameraMode.ADD) "Add Camera" else "Save Changes"
                )
            }
        }
    }
}

private fun setCamera(
    settingsViewModel: SettingsViewModel,
    context: Context,
    onBack: () -> Unit,
    mode: CameraMode,
    id: Int = 0,
    name: String,
    ip: String,
    port: Int,
    streamPort: Int,
    active: Boolean
) {
    if (name.isNotEmpty() && ip.isNotEmpty() && port != 0){
        settingsViewModel.viewModelScope.launch {
            val status =
                if(mode == CameraMode.ADD) {
                    settingsViewModel.insertCam(name, ip, port, streamPort, active)
                }
                else {
                    settingsViewModel.updateCam(id, name, ip, port, streamPort, active)
                }
            if (status == null)
                Toast.makeText(
                    context,
                    "IP already in use",
                    Toast.LENGTH_SHORT
                ).show()
            else if (!status) {
                Toast.makeText(
                    context,
                    "Max. 4 active cams allowed",
                    Toast.LENGTH_SHORT
                ).show()
                onBack()
            }
            else{
                Toast.makeText(
                    context,
                    "Camera added",
                    Toast.LENGTH_SHORT
                ).show()
                onBack()
            }
        }
    }
    else{
        Toast.makeText(
            context,
            "Please fill all fields",
            Toast.LENGTH_SHORT
        ).show()
    }
}