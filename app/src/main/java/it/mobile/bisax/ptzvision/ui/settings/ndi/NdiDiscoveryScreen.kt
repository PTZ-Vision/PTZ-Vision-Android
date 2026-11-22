package it.mobile.bisax.ptzvision.ui.settings.ndi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ndi.NdiDiscovery
import it.mobile.bisax.ptzvision.ndi.NdiRuntime
import it.mobile.bisax.ptzvision.ndi.NdiSource
import kotlinx.coroutines.delay

/** Lists the NDI® sources found on the network; tapping "Add" opens the add-camera form prefilled. */
@Composable
fun NdiDiscoveryScreen(
    existingCams: List<Cam>,
    onBack: () -> Unit,
    onAddSource: (NdiSource) -> Unit,
) {
    val context = LocalContext.current
    val discovery = remember { NdiDiscovery(context) }
    var extraIps by rememberSaveable { mutableStateOf("") }
    var appliedExtraIps by rememberSaveable { mutableStateOf("") }
    var sources by remember { mutableStateOf<List<NdiSource>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Debounce the extra-IP field: restarting the finder on each keystroke is wasteful.
    LaunchedEffect(extraIps) {
        delay(800)
        appliedExtraIps = extraIps
    }

    LaunchedEffect(appliedExtraIps) {
        error = null
        try {
            discovery.sources(appliedExtraIps).collect { sources = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Discovery failed"
        }
    }

    val configured = existingCams.filter { it.isNdi }.map { it.ndiSourceName }.toSet()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go Back")
                }
                Text(
                    text = "Find NDI® cameras",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                    fontSize = 24.sp
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            RuntimeBanner()

            OutlinedTextField(
                value = extraIps,
                onValueChange = { extraIps = it },
                label = { Text("Extra IPs (optional, comma separated)") },
                supportingText = { Text("For cameras on another subnet or where mDNS is blocked") },
                singleLine = true,
                enabled = NdiRuntime.isAvailable,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (sources.isEmpty() && error == null) {
                Text(
                    text = "Searching the local network…\nMake sure the phone is on the same network as the cameras.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sources, key = { it.fullName }) { source ->
                    SourceRow(
                        source = source,
                        alreadyAdded = source.fullName in configured,
                        onAdd = { onAddSource(source) }
                    )
                }
            }

            Text(
                text = "NDI® is a registered trademark of Vizrt NDI AB. ndi.video",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun RuntimeBanner() {
    val text = if (NdiRuntime.isAvailable) {
        "NDI runtime ${NdiRuntime.version ?: ""}".trim()
    } else {
        "NDI runtime not available (${NdiRuntime.unavailableReason ?: "unknown"}).\n" +
            "Showing mDNS results only: NDI PTZ control and video are disabled."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (NdiRuntime.isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SourceRow(source: NdiSource, alreadyAdded: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            .padding(16.dp, 8.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.sourceName,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(source.machineName.takeIf { it != source.sourceName }, source.address).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (alreadyAdded) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Added", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add")
            }
        }
    }
}
