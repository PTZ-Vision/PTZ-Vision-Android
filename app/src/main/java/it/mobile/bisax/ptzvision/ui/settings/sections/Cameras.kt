package it.mobile.bisax.ptzvision.ui.settings.sections

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun Camera(
    context: Context,
    settingsViewModel: SettingsViewModel,
    cam: Cam,
    onCameraModifyClick: (Cam) -> Unit
) {
    val openAlertDialog = remember { mutableStateOf(false) }
    var isCamActive = cam.active

    // AlertDialog
    when{
        openAlertDialog.value -> AlertDialog(
            title = {
                Text(text = "Elimina telecamera")
            },
            text = {
                val text = buildAnnotatedString {
                    append("Vuoi eliminare la telecamera ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)){
                        append(cam.name)
                    }
                    append("?")
                }
                Text(
                    text = text
                )
            },
            onDismissRequest = {
                openAlertDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.removeCam(cam.id)
                        openAlertDialog.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openAlertDialog.value = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 16.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                .padding(16.dp, 8.dp, 8.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(0.5f)
            ) {
                Text(
                    text = cam.name,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cam.ip + ":" + cam.port.toString(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Row(
                modifier = Modifier.weight(0.4f),
                horizontalArrangement = Arrangement.End
            ){
                IconButton(
                    onClick = {
                        isCamActive = !isCamActive

                        if (isCamActive) {
                            settingsViewModel.viewModelScope.launch {
                                isCamActive = settingsViewModel.addActive(cam.id)
                                if(!isCamActive)
                                    Toast.makeText(
                                        context,
                                        "Max. 4 active cams allowed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                            }
                        } else {
                            settingsViewModel.viewModelScope.launch {
                                isCamActive = !settingsViewModel.removeActive(cam.id)
                            }
                        }
                    },
                    modifier = Modifier
                        .padding()
                ){
                    Icon(
                        painter = painterResource(id = R.drawable.joystick),
                        contentDescription = "Add to console",
                        tint = if(isCamActive)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = { onCameraModifyClick(cam) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pen),
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                IconButton(onClick = {
                    openAlertDialog.value = true
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.trashbin),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CameraAddBtn(
    onAddClick: () -> Unit
){
    TextButton(
        onClick =onAddClick,
        modifier=Modifier.padding(16.dp,0.dp)
    ){
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
        )
        Text("Add", fontSize = 18.sp)
    }
}