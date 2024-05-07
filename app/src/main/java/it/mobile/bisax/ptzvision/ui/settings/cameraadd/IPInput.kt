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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun IPAddressInput(
    ip: String,
    onIpChange: (String) -> Unit
) {
    var ipAddressState by remember { mutableStateOf(ip) }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = ipAddressState,
            onValueChange = {
                ipAddressState = it
                onIpChange(it)
            },
            label = { Text("IP Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (!isValidIPAddress(ipAddressState) && ipAddressState.isNotEmpty()) {
            Text(
                text = "Invalid IP address",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

fun isValidIPAddress(ipAddress: String): Boolean {
    val pattern = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
    return if(!pattern.matches(ipAddress)){
        false
    } else {
        val parts = ipAddress.split(".")
        parts.all { it.toInt() in 0..255 }
    }
}

