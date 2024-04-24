package it.mobile.bisax.ptzvision.ui.settings

import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.data.cam.Cam


data class SettingsUiState(
    val layout: Layout = Layout.J_RIGHT,
    val cams: List<Cam> = emptyList(),
){
    enum class Layout(val layoutID: Int){
        J_RIGHT(R.drawable.layout_r),
        J_LEFT(R.drawable.layout_l);

        companion object {
            fun fromInt(value: Int) = entries.first { it.layoutID == value }
        }
    }
}


