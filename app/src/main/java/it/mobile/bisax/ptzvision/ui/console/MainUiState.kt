package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.ui.settings.SettingsUiState

data class MainUiState(
    val layout: SettingsUiState.Layout,
    val isAIEnabled: Boolean,
    val isAutoFocusEnabled: Boolean,
)
