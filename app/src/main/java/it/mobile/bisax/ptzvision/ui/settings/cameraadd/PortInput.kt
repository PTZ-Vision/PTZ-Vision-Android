package it.mobile.bisax.ptzvision.ui.settings.cameraadd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun PortInput(
    title: String,
    port: Int,
    onPortChange: (Int) -> Unit
) {
    var portState by remember { mutableIntStateOf(port) }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = TextFieldValue(
                text = portState.toString(),
                selection = TextRange(port.toString().length)
            ),
            onValueChange = {
                portState = it.text.toIntOrNull() ?: 0
                onPortChange(portState)
            },
            label = { Text(title) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        if (!isValidPort(portState.toString())) {
            Text(
                text = "Invalid port number",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

fun isValidPort(port: String): Boolean {
    val num = port.toIntOrNull() ?: return false
    return num in 0..65535
}