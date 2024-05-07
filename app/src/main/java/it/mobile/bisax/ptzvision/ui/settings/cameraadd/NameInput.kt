package it.mobile.bisax.ptzvision.ui.settings.cameraadd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CameraInput(
    name: String,
    onNameChange: (String) -> Unit
) {
    var nameState by remember { mutableStateOf(name) }

    Column (
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
    ){
        OutlinedTextField(
            value = nameState,
            onValueChange = {
                nameState = it
                onNameChange(it)
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}