package it.mobile.bisax.ptzvision.ui.settings.cameraadd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.data.utils.Protocol

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProtocolSelection(
    protocol: Protocol,
    onProtocolChange: (Protocol) -> Unit
) {
    var protocolState by remember { mutableStateOf(protocol) }
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            TextField(
                value = protocol.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("VISCA")
                    },
                    onClick = {
                        expanded = false
                        protocolState = Protocol.VISCA
                        onProtocolChange(protocolState)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text("CGI-HTTP")
                    }, onClick = {
                        expanded = false
                        protocolState = Protocol.CGI_HTTP
                        onProtocolChange(protocolState)
                    }
                )
            }
        }
    }
}