package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.util.Log
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

    /*private var joyStickParams by mutableStateOf(JoyStickPos(JoyStickDir.NONE, 0f))*/
    private var panIntensity by mutableFloatStateOf(0f)
    private var tiltIntensity by mutableFloatStateOf(0f)
    private var zoomIntensity by mutableFloatStateOf(0f)
    private var focusIntensity by mutableFloatStateOf(0f)

    /*fun setPTIntensity(maxRadius:Float = 1f, radius: Float = 0f, theta: Float= Float.NEGATIVE_INFINITY) {
        val intensity = radius / maxRadius
        var degree: Float = theta * 180f/ PI.toFloat()
        degree = (degree * 100f).roundToInt() / 100.0f

        val direction = when (degree) {
            in -22.5..22.5 -> JoyStickDir.RIGHT
            in 22.5..67.5 -> JoyStickDir.DOWN_RIGHT
            in 67.5..112.5 -> JoyStickDir.DOWN
            in 112.5..157.5 -> JoyStickDir.DOWN_LEFT
            in 157.5..180.0 -> JoyStickDir.LEFT
            in -180.0..-157.5 -> JoyStickDir.LEFT
            in -157.5..-112.5 -> JoyStickDir.UP_LEFT
            in -112.5..-67.5 -> JoyStickDir.UP
            in -67.5..-22.5 -> JoyStickDir.UP_RIGHT
            else -> JoyStickDir.NONE
        }

        joyStickParams = JoyStickPos(direction, intensity)
        Log.d("JoyStick", "JoyStickPos: $joyStickParams")
    }*/

    fun setPanTilt(xPos: Float, yPos: Float) {
        panIntensity = xPos
        tiltIntensity = yPos
        Log.d("PanTilt", "PanPos: $panIntensity, TiltPos: $tiltIntensity")
    }

    fun setZoomIntensity(maxPos: Float, posY: Float) {
        zoomIntensity = posY/maxPos
        Log.d("Zoom", "ZoomPos: $zoomIntensity")
    }

    fun setFocusIntensity(maxPos: Float, posY: Float) {
        focusIntensity = posY/maxPos
        Log.d("Focus", "FocusPos: $focusIntensity")
    }

    private fun setUIState(context: Context): MainUiState {
        val sharedPref = context.getSharedPreferences("MainUiState", Context.MODE_PRIVATE)

        val isAIEnabled = sharedPref.getBoolean("isAIEnabled", false)
        val isAutoFocusEnabled = sharedPref.getBoolean("isAutoFocusEnabled", false)
        val selectedCam = sharedPref.getInt("selectedCam", 0)

        return MainUiState(
            isAIEnabled = isAIEnabled,
            isAutoFocusEnabled = isAutoFocusEnabled,
            selectedCam = 0
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