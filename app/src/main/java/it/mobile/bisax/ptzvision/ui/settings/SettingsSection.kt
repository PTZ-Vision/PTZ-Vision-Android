package it.mobile.bisax.ptzvision.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    children: @Composable () -> Unit
) {
    Text(text = title)
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        children()
    }
}