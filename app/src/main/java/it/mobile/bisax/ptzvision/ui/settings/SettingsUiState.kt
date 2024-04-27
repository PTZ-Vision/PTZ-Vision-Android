package it.mobile.bisax.ptzvision.ui.settings

import it.mobile.bisax.ptzvision.data.cam.Cam


data class SettingsUiState(
    val layout: Layout = Layout.J_RIGHT,
    val cams: List<Cam> = emptyList(),
){
    enum class Layout(val layoutID: Int){
        J_RIGHT(0),
        J_LEFT(1);

        companion object {
            fun fromInt(value: Int) = entries.first { it.layoutID == value }
        }
    }
}


