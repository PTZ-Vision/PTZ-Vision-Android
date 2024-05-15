package it.mobile.bisax.ptzvision.ui.settings

import it.mobile.bisax.ptzvision.data.cam.Cam

data class SettingsUiState(
    var layout: Layout = Layout.J_RIGHT,
    var cams: List<Cam> = emptyList(),
    var currentCamName: String = "",
    var currentCamIP: String = "",
    var currentCamPort: Int = 0,
    var currentCamActive: Boolean = false,
){
    enum class Layout(val layoutID: Int){
        J_RIGHT(0),
        J_LEFT(1);

        companion object {
            fun fromInt(value: Int) = entries.first { it.layoutID == value }
        }
    }
}


