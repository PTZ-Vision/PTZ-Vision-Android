package it.mobile.bisax.ptzvision.ui.settings.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState
import it.mobile.bisax.ptzvision.ui.settings.SettingsViewModel

@Composable
fun Layout(
    settingsViewModel: SettingsViewModel
) {
    val textToEnableList: List<Int> = listOf(
        R.drawable.layout_r,
        R.drawable.layout_l
    )
    val selectedValue by settingsViewModel.settingsUiState.collectAsState()

    val isSelectedItem: (Int) -> Boolean = {
        if(selectedValue.layout == SettingsUiState.Layout.J_LEFT) {
            it == R.drawable.layout_l
        }
        else {
            it == R.drawable.layout_r
        }
    }
    val onChangeState: (Int) -> Unit = {
        if(it == R.drawable.layout_l)
            settingsViewModel.changeLayout(SettingsUiState.Layout.J_LEFT)
        else
            settingsViewModel.changeLayout(SettingsUiState.Layout.J_RIGHT)
    }

    Column(Modifier.padding(8.dp)) {
        textToEnableList.forEach { textToEnableState ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = isSelectedItem(textToEnableState),
                        onClick = { onChangeState(textToEnableState) }
                    )
                    .padding(8.dp)
            ) {
                RadioButton(
                    selected = isSelectedItem(textToEnableState),
                    onClick = null,
                    colors = RadioButtonColors(
                        selectedColor = Color.Blue,
                        unselectedColor = Color.Red,
                        disabledUnselectedColor = Color.LightGray,
                        disabledSelectedColor = Color.LightGray
                    )
                )
                Image(painter = painterResource(id = textToEnableState), contentDescription = "")
            }
        }
    }
}