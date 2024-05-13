package it.mobile.bisax.ptzvision.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel

class SettingsViewModelFactory(
    private val context: Context,
    private val camsViewModel: CamsViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context, camsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}