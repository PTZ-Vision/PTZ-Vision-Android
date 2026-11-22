package it.mobile.bisax.ptzvision.ui.settings

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamProtocol
import it.mobile.bisax.ptzvision.ndi.NdiRuntime
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    context: Context,
    private val camsViewModel: CamsViewModel
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _settingsUiState = MutableStateFlow(SettingsUiState())

    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        viewModelScope.launch {
            setUIState(appContext)
        }
    }

    fun changeLayout(layout: SettingsUiState.Layout) {
        _settingsUiState.update { currentState ->
            currentState.copy(layout = layout)
        }

        saveUIState(appContext, layout)
    }

    fun toggleHapticFeedback() {
        _settingsUiState.update { currentState ->
            currentState.copy(hapticFeedbackEnabled = !currentState.hapticFeedbackEnabled)
        }

        saveUIState(appContext, settingsUiState.value.layout)
    }

    fun toggleNdiHighBandwidth() {
        _settingsUiState.update { currentState ->
            currentState.copy(ndiHighBandwidth = !currentState.ndiHighBandwidth)
        }
        NdiRuntime.highBandwidthVideo = settingsUiState.value.ndiHighBandwidth

        saveUIState(appContext, settingsUiState.value.layout)
    }

    suspend fun insertCam(
        name: String, ip: String, controlPort: Int, streamPort: Int, httpPort: Int, active: Boolean,
        protocol: CamProtocol = CamProtocol.VISCA, ndiSourceName: String = ""
    ): Boolean? {
        val cam = Cam(
            name = name,
            ip = ip,
            controlPort = controlPort,
            streamPort = streamPort,
            httpPort = httpPort,
            active = false,
            protocol = protocol,
            ndiSourceName = if (protocol == CamProtocol.NDI) ndiSourceName.trim() else ""
        )

        val id = camsViewModel.addCam(cam)
        if(id == -1L) {
            return null
        }
        if (active) {
            return camsViewModel.addActive(id.toInt())
        }

        return true
    }

    suspend fun updateCam(
        id: Int, name: String, ip: String, controlPort: Int, streamPort: Int, httpPort: Int, active: Boolean,
        protocol: CamProtocol = CamProtocol.VISCA, ndiSourceName: String = ""
    ): Boolean? {
        val cam = Cam(
            id = id,
            name = name,
            ip = ip,
            controlPort = controlPort,
            streamPort = streamPort,
            httpPort = httpPort,
            active = active,
            protocol = protocol,
            ndiSourceName = if (protocol == CamProtocol.NDI) ndiSourceName.trim() else ""
        )

        try{
            camsViewModel.updateCam(cam)
            return true
        }
        catch (e: SQLiteConstraintException){
            return null
        }
    }

    fun removeCam(id: Int) {
        camsViewModel.removeCam(id)
    }

    suspend fun removeActive(id: Int): Boolean {
        return camsViewModel.removeActive(id)
    }

    suspend fun addActive(id: Int): Boolean {
        return camsViewModel.addActive(id)
    }

    private suspend fun setUIState(context: Context) {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val layout = sharedPref.getInt("layout", 0)
        val hapticFeedbackEnabled = sharedPref.getBoolean("hapticFeedbackEnabled", false)
        val ndiHighBandwidth = sharedPref.getBoolean("ndiHighBandwidth", false)
        NdiRuntime.highBandwidthVideo = ndiHighBandwidth

        camsViewModel.getAllCamsStream.collect { cams ->
            _settingsUiState.update {
                it.copy(
                    cams = cams,
                    layout = SettingsUiState.Layout.fromInt(layout),
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    ndiHighBandwidth = ndiHighBandwidth
                )
            }
        }
    }

    private fun saveUIState(context: Context, layout: SettingsUiState.Layout) {
        val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("layout", layout.layoutID)
            putBoolean("hapticFeedbackEnabled", settingsUiState.value.hapticFeedbackEnabled)
            putBoolean("ndiHighBandwidth", settingsUiState.value.ndiHighBandwidth)
            apply()
        }
    }

    suspend fun countActiveCams() : Int {
        return camsViewModel.countActiveCams()
    }
}