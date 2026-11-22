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

/** Zoom indicator text: "--x" when the camera cannot report its zoom position. */
fun MainUiState.zoomLabel(): String = if (zoomFeedback) "${zoomLevel}x" else "--x"
