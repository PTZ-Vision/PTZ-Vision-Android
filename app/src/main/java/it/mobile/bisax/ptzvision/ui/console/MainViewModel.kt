package it.mobile.bisax.ptzvision.ui.console

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun setNewActiveCam(camSlot: Int) {
        // Aggiorna subito le telecamere attive
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]

        _uiState.update {
            it.copy(activeCams = newActiveCams)
        }

        // Creazione asincrona del controller
        viewModelScope.launch(Dispatchers.IO) {
            val newController = try {
                ViscaPTZController(newActiveCams[0])
            } catch (e: Exception) {
                Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
                null
            }

            // Aggiornamento del controller nello stato dell'UI
            _uiState.update {
                it.copy(ptzController = newController)
            }
        }
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
            var zoomIntensity = posY / maxPos
            _uiState.value.ptzController?.zoom(MathUtils.clampUnit(zoomIntensity))
        }
    }

    suspend fun setFocusIntensity(maxPos: Float, posY: Float) {
        var focusIntensity = posY/maxPos

        //TODO: set focus intensity to camera
    }

    private fun getAIStatus(): Boolean {
        //TODO: get AI status from camera
        return false;
    }

    private fun getAutoFocusStatus(): Boolean {
        //TODO: get AutoFocus status from camera
        return false;
    }

    private fun setUIState() {
        viewModelScope.launch {
            camsViewModel.getActiveCamsStream.collect { cams ->
                // Aggiorna subito le telecamere attive
                _uiState.update {
                    it.copy(
                        isAIEnabled = getAIStatus(),
                        isAutoFocusEnabled = getAutoFocusStatus(),
                        activeCams = cams
                    )
                }

                // Creazione asincrona del controller
                val ptzController = try {
                    withContext(Dispatchers.IO) {
                        ViscaPTZController(cams[0])
                    }
                } catch (e: Exception) {
                    Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
                    null
                }

                // Aggiornamento del controller nello stato dell'UI
                _uiState.update {
                    it.copy(
                        ptzController = ptzController
                    )
                }
            }
        }
    }
}