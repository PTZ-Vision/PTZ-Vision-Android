package it.mobile.bisax.ptzvision.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import it.mobile.bisax.ptzvision.data.scene.ScenesViewModel

class MainViewModelFactory(
    private val camsViewModel: CamsViewModel,
    private val scenesViewModel: ScenesViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(camsViewModel, scenesViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}