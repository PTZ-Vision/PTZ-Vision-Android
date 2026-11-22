package it.mobile.bisax.ptzvision.controller

import android.util.Pair
import it.mobile.bisax.ptzvision.controller.PTZController.Result
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.ndi.NdiNative
import it.mobile.bisax.ptzvision.ndi.NdiReceiver
import it.mobile.bisax.ptzvision.ndi.NdiReceiverPool
import it.mobile.bisax.ptzvision.ndi.NdiRuntime
import java.io.Closeable
import java.io.IOException

/**
 * PTZ control through NDI® PTZ metadata messages (`ntk_ptz_*`), sent over the NDI connection.
 *
 * App conventions (same as [ViscaPTZController]): pan > 0 = right, tilt > 0 = up,
 * zoom > 0 = tele, focus > 0 = near; all in -1..1.
 * NDI conventions (Processing.NDI.Recv.ex.h): pan_speed > 0 = LEFT, tilt_speed > 0 = up,
 * zoom_speed > 0 = zoom in, focus_speed > 0 = focus inwards (near).
 */
class NdiPTZController(cam: Cam) : PTZController, Closeable {
    private val receiver: NdiReceiver = NdiReceiverPool.acquire(cam.ndiSourceName)
        ?: throw IOException(NdiRuntime.unavailableReason ?: "Cannot create NDI receiver for '${cam.ndiSourceName}'")

    private val handle get() = receiver.handle

    override fun move(pan: Float, tilt: Float): Result =
        send { NdiNative.ptzPanTiltSpeed(handle, ndiPanSpeed(pan), ndiSpeed(tilt)) }

    override fun zoom(zoom: Float): Result =
        send { NdiNative.ptzZoomSpeed(handle, ndiSpeed(zoom)) }

    override fun focus(focus: Float): Result =
        send { NdiNative.ptzFocusSpeed(handle, ndiSpeed(focus)) }

    override fun setAutoFocus(autoFocus: Boolean): Result = send {
        if (autoFocus) NdiNative.ptzAutoFocus(handle)
        // NDI has no "manual mode" message without a target: a zero-speed focus move switches
        // the camera to manual focus while keeping the current focus distance.
        else NdiNative.ptzFocusSpeed(handle, 0f)
    }

    // No standard NDI message for auto tracking.
    override fun setAutoTracking(autoTracking: Boolean): Result = Result.NOT_SUPPORTED

    override fun setScene(scene: Int): Result =
        if (scene !in PRESET_RANGE) Result.FAILURE else send { NdiNative.ptzStorePreset(handle, scene) }

    override fun callScene(scene: Int): Result =
        if (scene !in PRESET_RANGE) Result.FAILURE else send { NdiNative.ptzRecallPreset(handle, scene, 1f) }

    // NDI PTZ is a one-way protocol: the camera does not report its state.
    override fun getAutoFocus(): Pair<Result, Boolean> = Pair(Result.NOT_SUPPORTED, null)
    override fun getAutoTracking(): Pair<Result, Boolean> = Pair(Result.NOT_SUPPORTED, null)
    override fun getZoom(): Pair<Result, Double> = Pair(Result.NOT_SUPPORTED, null)

    override fun close() = receiver.release()

    private inline fun send(command: () -> Boolean): Result =
        if (receiver.isReleased) Result.FAILURE else if (command()) Result.SUCCESS else Result.FAILURE

    companion object {
        /** NDI preset indices are 0..99. */
        val PRESET_RANGE = 0..99

        /** Clamps to -1..1 and normalises -0.0 (sent as "-0.000000" otherwise) and NaN to 0. */
        fun ndiSpeed(value: Float): Float = if (value == 0f || value.isNaN()) 0f else value.coerceIn(-1f, 1f)

        /** App pan (> 0 = right) to NDI pan_speed (> 0 = left). */
        fun ndiPanSpeed(pan: Float): Float = ndiSpeed(-ndiSpeed(pan))
    }
}
