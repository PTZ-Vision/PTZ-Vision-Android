package it.mobile.bisax.ptzvision.ui.settings

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _camsViewModel = CamsViewModel(context)
    private val _settingsUiState = MutableStateFlow(setUIState(context))

    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState

    fun changeLayout(layout: SettingsUiState.Layout) {
        _settingsUiState.update { currentState ->
            currentState.copy(layout = layout)
        }

        saveUIState(appContext, layout)
    }

    suspend fun insertCam(name: String, ip: String, port: Int, active: Boolean): Boolean? {
        val cam = Cam(
            name = name,
            ip = ip,
            port = port,
            active = false
        )

        val id = _camsViewModel.addCam(cam)
        if(id == -1L) {
            return null
        }
        if (active) {
            return _camsViewModel.addActive(id.toInt())
        }

        return true
    }

    suspend fun updateCam(id: Int, name: String, ip: String, port: Int, active: Boolean): Boolean? {
        val cam = Cam(
            id = id,
            name = name,
            ip = ip,
            port = port,
            active = active
        )

        try{
            _camsViewModel.updateCam(cam)
            return true
        }
        catch (e: SQLiteConstraintException){
            return null
        }
    }

    fun removeCam(id: Int) {
        _camsViewModel.removeCam(id)
    }

    suspend fun removeActive(id: Int): Boolean {
        return _camsViewModel.removeActive(id)
    }

    suspend fun addActive(id: Int): Boolean {
        return _camsViewModel.addActive(id)
    }

    private fun setUIState(context: Context): SettingsUiState {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)

        val layout = sharedPref.getInt("layout", 0)
        val uiState = SettingsUiState(
            SettingsUiState.Layout.fromInt(layout)
        )
        viewModelScope.launch {
            _camsViewModel.getAllCamsStream.collect { cams ->
                _settingsUiState.update {
                    it.copy(cams = cams)
                }
            }
        }

        return uiState
    }

    private fun saveUIState(context: Context, layout: SettingsUiState.Layout) {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("layout", layout.layoutID)
            apply()
        }
    }
}