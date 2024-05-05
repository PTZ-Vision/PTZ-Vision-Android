package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(setUIState(appContext))

    val uiState: StateFlow<MainUiState> = _uiState

    fun toggleAI() {
        _uiState.update {currentState ->
            currentState.copy(isAIEnabled = !currentState.isAIEnabled)
        }
        saveUIState(appContext)
    }

    fun toggleAutoFocus() {
        _uiState.update {currentState ->
            currentState.copy(isAutoFocusEnabled = !currentState.isAutoFocusEnabled)
        }
        saveUIState(appContext)
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

    private fun setUIState(context: Context): MainUiState {
        val sharedPref = context.getSharedPreferences("MainUiState", Context.MODE_PRIVATE)

        val isAIEnabled = sharedPref.getBoolean("isAIEnabled", false)
        val isAutoFocusEnabled = sharedPref.getBoolean("isAutoFocusEnabled", false)

        return MainUiState(
            isAIEnabled = isAIEnabled,
            isAutoFocusEnabled = isAutoFocusEnabled
        )
    }

    private fun saveUIState(context: Context) {
        val sharedPref = context.getSharedPreferences("MainUiState", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isAIEnabled", uiState.value.isAIEnabled)
            putBoolean("isAutoFocusEnabled", uiState.value.isAutoFocusEnabled)
            apply()
        }
    }
}