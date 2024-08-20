package it.mobile.bisax.ptzvision.ui.console

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.controller.ViscaPTZController
import it.mobile.bisax.ptzvision.controller.utils.MathUtils
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val camsViewModel: CamsViewModel
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            setUIState()
        }
    }

    suspend fun toggleAI() {
        _uiState.update {currentState ->
            currentState.copy(isAIEnabled = !currentState.isAIEnabled)
        }

        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.setAutoTracking(_uiState.value.isAIEnabled)
        }
    }

    suspend fun toggleAutoFocus() {
        _uiState.update {currentState ->
            currentState.copy(isAutoFocusEnabled = !currentState.isAutoFocusEnabled)
        }
        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.setAutoFocus(_uiState.value.isAutoFocusEnabled)
        }
    }

    fun initPTZController() {
        viewModelScope.launch(Dispatchers.IO) {
            val newController = try {
                ViscaPTZController(_uiState.value.activeCams[0])
            } catch (e: Exception) {
                Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
                null
            }

            _uiState.update {
                it.copy(
                    ptzController = newController,
                )
            }

            _uiState.update {
                it.copy(
                    isAIEnabled = getAIStatus(),
                    zoomLevel = getZoomLevel(),
                    isAutoFocusEnabled = getAutoFocusStatus()
                )
            }
        }
    }

    fun resetPTZController() {
        (_uiState.value.ptzController as ViscaPTZController?)?.close()
        _uiState.update {
            it.copy(ptzController = null)
        }
    }

    fun setNewActiveCam(camSlot: Int) {
        // Aggiorna subito le telecamere attive
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]

        _uiState.update {
            it.copy(
                activeCams = newActiveCams,
                ptzController = null
            )
        }

        initPTZController()
    }

    suspend fun goToScene(sceneSlot: Int){
        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.callScene(sceneSlot)
        }
    }

    suspend fun saveSceneOnCam(sceneSlot: Int){
        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.setScene(sceneSlot)
        }
    }


    suspend fun setPanTilt(xPos: Float, yPos: Float) {
        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.move(MathUtils.clampUnit(xPos), MathUtils.clampUnit(yPos))
        }
    }

    suspend fun setZoomIntensity(maxPos: Float, posY: Float) {
        withContext(Dispatchers.IO) {
            val zoomIntensity = posY / maxPos
            _uiState.value.ptzController?.zoom(MathUtils.clampUnit(zoomIntensity))

            // TO FIX: This is a workaround to update the zoom level in the UI
            var newZoom = getZoomLevel()
            if (newZoom == 0.0 || newZoom > 30.0) newZoom = _uiState.value.zoomLevel
            _uiState.update {
                it.copy(zoomLevel = newZoom)
            }
        }
    }

    suspend fun setFocusIntensity(maxPos: Float, posY: Float) {
        var focusIntensity = posY/maxPos
        withContext(Dispatchers.IO) {
            val focusIntensity = posY / maxPos
            _uiState.value.ptzController?.focus(MathUtils.clampUnit(focusIntensity))
        }
    }

    private fun getAIStatus(): Boolean {
        val controller = _uiState.value.ptzController
        if(controller == null) {
            Log.e("MainViewModel", "PTZController is null while getting AI status")
            return false
        }
        return controller.getAutoTracking()?.second ?: false
    }

    private fun getAutoFocusStatus(): Boolean {
        val controller = _uiState.value.ptzController
        if(controller == null) {
            Log.e("MainViewModel", "PTZController is null while getting AutoFocus status")
            return false
        }
        return controller.getAutoFocus()?.second ?: false
    }

    private fun getZoomLevel(): Double {
        val controller = _uiState.value.ptzController
        if(controller == null) {
            Log.e("MainViewModel", "PTZController is null while getting Zoom level")
            return 1.0
        }
        return controller.getZoom()?.second ?: 0.0
    }

    private fun setUIState() {
        viewModelScope.launch {
            camsViewModel.getActiveCamsStream.collect { cams ->
                // Aggiorna subito le telecamere attive
                _uiState.update {
                    it.copy(
                        isAIEnabled = getAIStatus(),
                        zoomLevel = getZoomLevel(),
                        isAutoFocusEnabled = getAutoFocusStatus(),
                        activeCams = cams,
                        ptzController = null
                    )
                }
            }
        }
    }
}