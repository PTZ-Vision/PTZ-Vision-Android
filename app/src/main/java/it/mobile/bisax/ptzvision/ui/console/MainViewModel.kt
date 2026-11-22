package it.mobile.bisax.ptzvision.ui.console

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.controller.NdiPTZController
import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.controller.ViscaPTZController
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamProtocol
import it.mobile.bisax.ptzvision.controller.utils.MathUtils
import it.mobile.bisax.ptzvision.data.cam.CamsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

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
            val cam = _uiState.value.activeCams.getOrNull(0) ?: return@launch
            val newController = try {
                createController(cam)
            } catch (e: Exception) {
                Log.d("MainViewModel", "Error creating PTZController: ${e.message}")
                null
            }

            var stale = false
            _uiState.update {
                // The selected camera changed while we were connecting: drop this controller.
                if (it.activeCams.getOrNull(0) != cam || it.ptzController != null) {
                    stale = true
                    it
                } else it.copy(ptzController = newController)
            }
            if (stale) {
                closeController(newController)
                return@launch
            }

            if(newController == null)
                return@launch

            val zoomFeedback = newController.zoom.first != PTZController.Result.NOT_SUPPORTED
            var zoom = if (zoomFeedback) getZoomLevel() else 0.0
            if (zoomFeedback && (zoom == 0.0 || zoom > 30.0)) {
                zoom = _uiState.value.zoomLevel
            }

            _uiState.update {
                it.copy(
                    isAIEnabled = getAIStatus(),
                    zoomLevel = zoom,
                    zoomFeedback = zoomFeedback,
                    isAutoFocusEnabled = getAutoFocusStatus()
                )
            }
        }
    }

    fun resetPTZController() {
        val old = _uiState.value.ptzController
        _uiState.update {
            it.copy(ptzController = null)
        }
        closeController(old)
    }

    private fun createController(cam: Cam): PTZController = when (cam.protocol) {
        CamProtocol.NDI -> NdiPTZController(cam)
        CamProtocol.VISCA -> ViscaPTZController(cam)
    }

    private fun closeController(controller: PTZController?) {
        if (controller !is Closeable) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { controller.close() }
                .onFailure { Log.d("MainViewModel", "Error closing PTZController: ${it.message}") }
        }
    }

    override fun onCleared() {
        // viewModelScope is already cancelled here: close synchronously (both closes are cheap).
        runCatching { (_uiState.value.ptzController as? Closeable)?.close() }
        super.onCleared()
    }

    fun setNewActiveCam(camSlot: Int) {
        // Aggiorna subito le telecamere attive
        val newActiveCams = _uiState.value.activeCams.toMutableList()
        newActiveCams[0] = _uiState.value.activeCams[camSlot]
        newActiveCams[camSlot] = _uiState.value.activeCams[0]

        val old = _uiState.value.ptzController
        _uiState.update {
            it.copy(
                activeCams = newActiveCams,
                ptzController = null
            )
        }
        closeController(old)

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
            if (!_uiState.value.zoomFeedback) return@withContext

            // TO FIX: This is a workaround to update the zoom level in the UI
            var newZoom = getZoomLevel()
            if (newZoom == 0.0 || newZoom > 30.0) newZoom = _uiState.value.zoomLevel
            _uiState.update {
                it.copy(zoomLevel = newZoom)
            }
        }
    }

    suspend fun setFocusIntensity(maxPos: Float, posY: Float) {
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
            return _uiState.value.zoomLevel
        }
        return controller.getZoom()?.second ?: 0.0
    }

    suspend fun updateZoomLevel() {
        withContext(Dispatchers.IO) {
            if (!_uiState.value.zoomFeedback) return@withContext
            val zoom = getZoomLevel()
            if(zoom == 0.0 || zoom > 30.0)
                return@withContext

            _uiState.update {
                it.copy(zoomLevel = zoom)
            }
        }
    }

    private fun setUIState() {
        viewModelScope.launch {
            var zoom = getZoomLevel()
            if (zoom == 0.0 || zoom > 30.0) {
                zoom = _uiState.value.zoomLevel
            }

            camsViewModel.getActiveCamsStream.collect { cams ->
                closeController(_uiState.value.ptzController)
                // Aggiorna subito le telecamere attive
                _uiState.update {
                    it.copy(
                        isAIEnabled = getAIStatus(),
                        zoomLevel = zoom,
                        isAutoFocusEnabled = getAutoFocusStatus(),
                        activeCams = cams,
                        ptzController = null
                    )
                }
            }
        }
    }
}