package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.data.cam.Cam

data class MainUiState(
    val isAIEnabled: Boolean = false,
    val isAutoFocusEnabled: Boolean = false,
    val activeCams: List<Cam?> = emptyList(),
    val zoomLevel: Double = 0.0,
    /** False when the protocol cannot report the zoom position (e.g. NDI PTZ is one-way). */
    val zoomFeedback: Boolean = true,
    val ptzController: PTZController? = null
)

/** Only display a measured zoom level for the selected camera. */
fun MainUiState.zoomLabel(): String? =
    if (zoomFeedback && zoomLevel > 0.0 && zoomLevel.isFinite()) "${zoomLevel}x" else null
