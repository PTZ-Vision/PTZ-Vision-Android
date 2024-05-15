package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.scene.Scene

data class MainUiState(
    val isAIEnabled: Boolean,
    val isAutoFocusEnabled: Boolean,
    val activeCams: List<Cam?> = emptyList(),
    val selectedCamSlot: Int = 0,
    val camScenes: List<Scene> = emptyList(),
)
