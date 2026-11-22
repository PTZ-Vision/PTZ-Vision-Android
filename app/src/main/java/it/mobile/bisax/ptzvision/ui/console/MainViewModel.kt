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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

class MainViewModel(
    private val camsViewModel: CamsViewModel
) : ViewModel() {
    private val trackingMutex = Mutex()
    private val focusMutex = Mutex()
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            setUIState()
        }
    }

    suspend fun setAITracking(enabled: Boolean): Boolean {
        val controller = _uiState.value.ptzController ?: return false
        return withContext(Dispatchers.IO) {
            trackingMutex.withLock {
                if (_uiState.value.ptzController !== controller) return@withLock false
                val success = controller.setAutoTracking(enabled) == PTZController.Result.SUCCESS
                if (success) {
                    _uiState.update { if (it.ptzController === controller) it.copy(isAIEnabled = enabled) else it }
                }
                success
            }
        }
    }

    suspend fun toggleAutoFocus() {
        val controller = _uiState.value.ptzController ?: return
        withContext(Dispatchers.IO) {
            focusMutex.withLock {
                val state = _uiState.value
                if (state.ptzController !== controller || state.isAIEnabled) return@withLock
                val enabled = !state.isAutoFocusEnabled
                if (controller.setAutoFocus(enabled) == PTZController.Result.SUCCESS) {
                    _uiState.update { if (it.ptzController === controller) it.copy(isAutoFocusEnabled = enabled) else it }
                }
            }
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
                } else it.copy(ptzController = newController, isAutoFocusEnabled = false, isAIEnabled = false)
            }
            if (stale) {
                closeController(newController)
                return@launch
            }

            if(newController == null)
                return@launch

            trackingMutex.withLock {
                val tracking = newController.autoTracking.second
                if (tracking != null) {
                    _uiState.update { if (it.ptzController === newController) it.copy(isAIEnabled = tracking) else it }
                }
            }
            val reading = newController.zoom
            _uiState.update {
                if (it.ptzController !== newController) return@update it
                it.copy(
                    zoomLevel = reading.second?.takeIf { level -> level > 0.0 && level <= 30.0 } ?: 0.0,
                    zoomFeedback = reading.first != PTZController.Result.NOT_SUPPORTED
                )
            }
            focusMutex.withLock {
                val enabled = newController.autoFocus.second ?: return@withLock
                _uiState.update { if (it.ptzController === newController) it.copy(isAutoFocusEnabled = enabled) else it }
            }
        }
    }

    fun resetPTZController() {
        val old = _uiState.value.ptzController
        _uiState.update {
            it.copy(ptzController = null, zoomLevel = 0.0)
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
                zoomLevel = 0.0,
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
            if (_uiState.value.isAIEnabled) return@withContext
            _uiState.value.ptzController?.move(MathUtils.clampUnit(xPos), MathUtils.clampUnit(yPos))
        }
    }

    suspend fun setZoomIntensity(maxPos: Float, posY: Float) {
        withContext(Dispatchers.IO) {
            if (_uiState.value.isAIEnabled) return@withContext
            val zoomIntensity = posY / maxPos
            val controller = _uiState.value.ptzController ?: return@withContext
            controller.zoom(MathUtils.clampUnit(zoomIntensity))
            refreshZoom(controller)
        }
    }

    suspend fun setFocusIntensity(maxPos: Float, posY: Float) {
        val controller = _uiState.value.ptzController ?: return
        withContext(Dispatchers.IO) {
            focusMutex.withLock {
                val state = _uiState.value
                // NDI treats even a delayed slider stop as a manual-focus command.
                if (state.ptzController === controller && !state.isAIEnabled && !state.isAutoFocusEnabled) {
                    controller.focus(MathUtils.clampUnit(posY / maxPos))
                }
            }
        }
    }

    private fun refreshZoom(controller: PTZController) {
        if (!_uiState.value.zoomFeedback) return
        val zoom = controller.zoom.second ?: return
        if (!(zoom > 0.0 && zoom <= 30.0)) return
        _uiState.update {
            if (it.ptzController === controller) it.copy(zoomLevel = zoom) else it
        }
    }

    suspend fun updateZoomLevel() {
        withContext(Dispatchers.IO) {
            _uiState.value.ptzController?.let { refreshZoom(it) }
        }
    }

    private fun setUIState() {
        viewModelScope.launch {
            camsViewModel.getActiveCamsStream.collect { cams ->
                closeController(_uiState.value.ptzController)
                // Aggiorna subito le telecamere attive
                _uiState.update {
                    it.copy(
                        isAIEnabled = false,
                        zoomLevel = 0.0,
                        isAutoFocusEnabled = false,
                        activeCams = cams,
                        ptzController = null
                    )
                }
            }
        }
    }
}