package it.mobile.bisax.ptzvision.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import it.mobile.bisax.ptzvision.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _settingsUiState = MutableStateFlow(setUIState(appContext))

    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState

    fun changeLayout(layoutID: Int) {
        val layout: SettingsUiState.Layout = SettingsUiState.Layout.fromInt(layoutID)

        _settingsUiState.update{currentState ->
            currentState.copy(layout = layout)
        }

        saveUIState(appContext)
    }

    private fun setUIState(context: Context): SettingsUiState {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)

        val layout = sharedPref.getInt("layout", R.drawable.layout_r)

        return SettingsUiState(
            layout = SettingsUiState.Layout.fromInt(layout)
        )
    }

    private fun saveUIState(context: Context) {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("layout", settingsUiState.value.layout.layoutID)
            apply()
        }
    }
}