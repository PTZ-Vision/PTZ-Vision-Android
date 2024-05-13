package it.mobile.bisax.ptzvision.data.cam

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CamsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CamsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CamsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}