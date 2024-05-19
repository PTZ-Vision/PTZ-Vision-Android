package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.data.cam.Cam

data class MainUiState(
    val isAIEnabled: Boolean = false,
    val isAutoFocusEnabled: Boolean = false,
    val activeCams: List<Cam?> = emptyList(),
    val ptzController: PTZController? = null
)
