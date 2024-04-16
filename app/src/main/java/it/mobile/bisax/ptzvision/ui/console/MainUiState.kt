package it.mobile.bisax.ptzvision.ui.console

data class ButtonData(
    val label: String,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
)

data class MainUiState(
    val isAIEnabled: Boolean,
    val isAutoFocusEnabled: Boolean,
    //val buttons: MutableList<ButtonData>
)
