package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.controller.HttpCgiPTZController
import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.controller.ViscaPTZController
import it.mobile.bisax.ptzvision.controller.utils.MathUtils
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import it.mobile.bisax.ptzvision.data.utils.Protocol
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

    fun getSelectedCam(): Cam {
        return _uiState.value.activeCams[0]!!
    }

    suspend fun toggleAI() {
        _uiState.update {currentState ->
            currentState.copy(isAIEnabled = !currentState.isAIEnabled)
        }
        // TODO: send command to camera
    }

    suspend fun toggleAutoFocus() {
        _uiState.update {currentState ->
            currentState.copy(isAutoFocusEnabled = !currentState.isAutoFocusEnabled)
        }
        // TODO: send command to camera
    }

    fun setNewActiveCam(camSlot: Int) {
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]

        var ptzController : PTZController?
        try{
            // ptzController = ViscaPTZController(newActiveCams[0])
            ptzController = HttpCgiPTZController(newActiveCams[0])
        } catch(e: Exception) {
            Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
            ptzController = null
        }

        _uiState.update {
            it.copy(
                activeCams = newActiveCams,
                ptzController = ptzController
            )
        }
    }

    suspend fun goToScene(sceneSlot: Int){
        //TODO: set position of camera to scene slot
    }

    suspend fun saveSceneOnCam(sceneSlot: Int){
        //TODO: save current camera position to scene slot
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
            camsViewModel.getActiveCamsStream.collect{cams ->
                var ptzController: PTZController?
                try{
                    ptzController = withContext(Dispatchers.IO) { // Move network operation to background thread
                        ViscaPTZController(cams[0])
                    }
                } catch (e: Exception){
                    Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
                    ptzController = null
                }
                _uiState.update {
                    it.copy(
                        isAIEnabled = getAIStatus(),
                        isAutoFocusEnabled = getAutoFocusStatus(),
                        activeCams = cams,
                        ptzController = ptzController
                    )
                }
            }
        }
    }

}