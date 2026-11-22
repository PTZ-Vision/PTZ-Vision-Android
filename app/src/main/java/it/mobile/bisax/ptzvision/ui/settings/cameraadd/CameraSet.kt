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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.CamProtocol
import it.mobile.bisax.ptzvision.ndi.NdiRuntime
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

enum class CameraMode {
    ADD,
    MODIFY
}

const val VISCA_PORT = 5678
const val RTSP_PORT = 554
const val HTTP_PORT = 80

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
    camControlPort: Int = VISCA_PORT,
    camStreamPort: Int = RTSP_PORT,
    camHttpPort: Int = HTTP_PORT,
    camActive: Boolean = false,
    camProtocol: CamProtocol = CamProtocol.VISCA,
    camNdiSourceName: String = ""
) {
    // lock orientation to vertical
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        var name by remember { mutableStateOf(camName) }
        var ip by remember { mutableStateOf(camIp) }
        var controlPort by remember { mutableIntStateOf(camControlPort) }
        var streamPort by remember { mutableIntStateOf(camStreamPort) }
        var httpPort by remember { mutableIntStateOf(camHttpPort) }
        var active by remember { mutableStateOf(camActive) }
        var protocol by remember { mutableStateOf(camProtocol) }
        var ndiSourceName by remember { mutableStateOf(camNdiSourceName) }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    text = if (mode == CameraMode.ADD) "Add a Camera" else "Modify a Camera",
                    modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                    style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp)
                )
            }
            ProtocolSelector(protocol = protocol) { protocol = it }
            CameraInput(name = name) {
                name = it
            }
            if (protocol == CamProtocol.NDI) {
                NdiSourceInput(sourceName = ndiSourceName) { ndiSourceName = it }
                Text(
                    text = "AI Tracking uses the VISCA control port on compatible cameras. The optional IP is used for tracking, RTSP " +
                        "fallback and snapshots." +
                        if (!NdiRuntime.isAvailable) "\nNDI runtime not available on this build: PTZ control over NDI is disabled." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            IPAddressInput(ip = ip) {
                ip = it
            }
            PortInput(title = if (protocol == CamProtocol.NDI) "VISCA Port (AI Tracking)" else "Control Port", port = controlPort) {
                controlPort = it
            }
            PortInput(title = if (protocol == CamProtocol.NDI) "RTSP Port (fallback)" else "Streaming Port", port = streamPort) {
                streamPort = it
            }
            PortInput(title = "HTTP Port", port = httpPort) {
                httpPort = it
            }

            Button(
                onClick = {
                    active = !active
                },
                modifier = Modifier.padding(16.dp),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.let {
                        if(!active)
                            it.copy(alpha = 0.5f)
                        else
                            it
                    },
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer.let {
                        if(!active)
                            it.copy(alpha = 0.5f)
                        else
                            it
                    },
                    disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.joystick),
                    contentDescription = "Add to console",
                    tint = if (active)
                        Color.Yellow
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    text = if (!active) "Add to Console" else "Remove from console",
                    modifier = Modifier.padding(start = 8.dp)
                )
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
                        controlPort = controlPort,
                        streamPort = streamPort,
                        httpPort = httpPort,
                        active = active,
                        protocol = protocol,
                        ndiSourceName = ndiSourceName
                    )
                },
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text(
                    text = if (mode == CameraMode.ADD) "Add Camera" else "Save Changes"
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
    controlPort: Int,
    streamPort: Int,
    httpPort: Int,
    active: Boolean,
    protocol: CamProtocol,
    ndiSourceName: String
) {
    val valid = when (protocol) {
        CamProtocol.VISCA -> name.isNotEmpty() && ip.isNotEmpty() && controlPort != 0
        CamProtocol.NDI -> name.isNotEmpty() && ndiSourceName.isNotBlank()
    }
    if (valid) {
        settingsViewModel.viewModelScope.launch {
            val status =
                if (mode == CameraMode.ADD) {
                    settingsViewModel.insertCam(name, ip, controlPort, streamPort, httpPort, active, protocol, ndiSourceName)
                } else {
                    val countActive = settingsViewModel.countActiveCams()
                    if (countActive>=4) {
                        settingsViewModel.updateCam(id, name, ip, controlPort, streamPort, httpPort, false, protocol, ndiSourceName)
                        false
                    } else {
                        settingsViewModel.updateCam(id, name, ip, controlPort, streamPort, httpPort, active, protocol, ndiSourceName)
                    }
                }

            if (status == null)
                Toast.makeText(
                    context,
                    if (protocol == CamProtocol.NDI) "This NDI source is already configured" else "IP & ports combination already in use",
                    Toast.LENGTH_SHORT
                ).show()
            else if (!status) {
                Toast.makeText(
                    context,
                    "Max. 4 active cams allowed",
                    Toast.LENGTH_SHORT
                ).show()
                onBack()
            } else {
                Toast.makeText(
                    context,
                    "Camera ${if (mode == CameraMode.ADD) "added" else "modified"}",
                    Toast.LENGTH_SHORT
                ).show()
                onBack()
            }
        }
    } else {
        Toast.makeText(
            context,
            "Please fill all fields",
            Toast.LENGTH_SHORT
        ).show()
    }
}
@Composable
private fun ProtocolSelector(protocol: CamProtocol, onChange: (CamProtocol) -> Unit) {
    val options = CamProtocol.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = protocol == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(if (option == CamProtocol.NDI) "NDI®" else "VISCA over IP")
            }
        }
    }
}

@Composable
private fun NdiSourceInput(sourceName: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = sourceName,
        onValueChange = onChange,
        label = { Text("NDI source name") },
        placeholder = { Text("MACHINE (SOURCE)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}
