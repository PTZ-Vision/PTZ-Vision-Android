package it.mobile.bisax.ptzvision.ui.console

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun toggleAI() {
        _uiState.update {currentState ->
            currentState.copy(isAIEnabled = !currentState.isAIEnabled)
        }
        // TODO: send command to camera
    }

    fun toggleAutoFocus() {
        _uiState.update {currentState ->
            currentState.copy(isAutoFocusEnabled = !currentState.isAutoFocusEnabled)
        }
        // TODO: send command to camera
    }

    fun setNewActiveCam(camSlot: Int) {
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]
        _uiState.update {
            it.copy(
                activeCams = newActiveCams
            )
        }
    }

    fun goToScene(sceneSlot: Int){
        //TODO: set position of camera to scene slot
    }

    fun saveSceneOnCam(sceneSlot: Int){
        //TODO: save current camera position to scene slot
    }

    private var panIntensity by mutableFloatStateOf(0f)
    private var tiltIntensity by mutableFloatStateOf(0f)
    private var zoomIntensity by mutableFloatStateOf(0f)
    private var focusIntensity by mutableFloatStateOf(0f)

    fun setPanTilt(xPos: Float, yPos: Float) {
        panIntensity = xPos
        tiltIntensity = yPos
    }

    fun setZoomIntensity(maxPos: Float, posY: Float) {
        zoomIntensity = posY/maxPos
    }

    fun setFocusIntensity(maxPos: Float, posY: Float) {
        focusIntensity = posY/maxPos
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
            camsViewModel.getActiveCamsStream.collect{cams ->
                _uiState.update {
                    it.copy(
                        isAIEnabled = getAIStatus(),
                        isAutoFocusEnabled = getAutoFocusStatus(),
                        activeCams = cams,
                    )
                }
            }
        }
    }

}