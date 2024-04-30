package it.mobile.bisax.ptzvision.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSection(
    title: String
) {
    HorizontalDivider()
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight(600),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 16.dp, 0.dp, 0.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}