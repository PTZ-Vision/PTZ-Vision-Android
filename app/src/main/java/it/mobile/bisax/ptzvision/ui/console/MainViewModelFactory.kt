package it.mobile.bisax.ptzvision.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel

class MainViewModelFactory(
    private val camsViewModel: CamsViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(camsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}