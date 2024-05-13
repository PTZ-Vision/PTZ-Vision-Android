package it.mobile.bisax.ptzvision.ui.console

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import it.mobile.bisax.ptzvision.data.scene.Scene
import it.mobile.bisax.ptzvision.data.scene.ScenesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val camsViewModel: CamsViewModel,
    private val scenesViewModel: ScenesViewModel
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState(
        isAIEnabled = false,
        isAutoFocusEnabled = false
    ))
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

    // TODO: BUG GRAFICO, RESET UI STATE
    suspend fun setNewActiveCam(camSlot: Int, camId: Int) {
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]

        scenesViewModel.getScenesByCam(camId = camId).collect{ scenes ->
            _uiState.update {
                it.copy(
                    camScenes = scenes,
                    activeCams = newActiveCams
                )
            }
        }
    }

    fun updateScene(sceneId: Int, slot: Int, name: String, camId: Int) {
        Log.d("PTZD_UPDATE", "Update scene $slot $sceneId $name")
        viewModelScope.launch {
            scenesViewModel.updateScene(
                Scene(
                    id = sceneId,
                    idCamera = camId,
                    slot = slot,
                    name = name,
                    pan = Math.random().toFloat(),
                    tilt = Math.random().toFloat(),
                    zoom = Math.random().toFloat(),
                    focus = Math.random().toFloat(),
                    iris = Math.random().toFloat(),
                    panSpeed = Math.random().toFloat(),
                    tiltSpeed = Math.random().toFloat(),
                    zoomSpeed = Math.random().toFloat(),
                    focusSpeed = Math.random().toFloat(),
                )
            )
        }
    }

    fun addScene(slot: Int, camId: Int) {
        Log.d("PTZD_ADD", "Add scene $slot $camId")

        if(camId == 0)
            return

        viewModelScope.launch {
            scenesViewModel.addScene(
                Scene(
                    idCamera = camId,
                    slot = slot,
                    name = "$camId - $slot",
                    pan = Math.random().toFloat(),
                    tilt = Math.random().toFloat(),
                    zoom = Math.random().toFloat(),
                    focus = Math.random().toFloat(),
                    iris = Math.random().toFloat(),
                    panSpeed = Math.random().toFloat(),
                    tiltSpeed = Math.random().toFloat(),
                    zoomSpeed = Math.random().toFloat(),
                    focusSpeed = Math.random().toFloat(),
                )
            )
            scenesViewModel.getScenesByCam(camId).collect { scenes ->
                _uiState.update {
                    it.copy(
                        camScenes = scenes
                    )
                }
            }
        }
    }

    fun sendSceneToDevice(scene: Scene){
        Log.d("PTZDScene${scene.name}", scene.toString())
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

    private suspend fun setUIState() {
        val cameras: List<Cam> = camsViewModel.getActiveCamsStream.first()

        scenesViewModel.getScenesByCam(cameras[0].id).collect { scenes ->
            _uiState.update {
                it.copy(
                    camScenes = scenes,
                    activeCams = cameras + List(4 - cameras.size) { null }
                )
            }
        }
    }
}