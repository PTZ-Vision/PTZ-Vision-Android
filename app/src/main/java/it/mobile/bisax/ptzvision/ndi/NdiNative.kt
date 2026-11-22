package it.mobile.bisax.ptzvision.ndi

import android.view.Surface

/**
 * JNI entry points implemented in `src/main/cpp/ndi_bridge.cpp`.
 * Always go through [NdiRuntime] / [NdiReceiverPool] instead of calling these directly.
 */
internal object NdiNative {
    /** True when the JNI bridge itself (libndibridge.so) could be loaded. */
    val bridgeLoaded: Boolean = runCatching { System.loadLibrary("ndibridge") }.isSuccess

    /** Loads libndi.so and initialises it. Returns null on success, otherwise the error. */
    external fun nativeInit(): String?
    external fun nativeVersion(): String?

    external fun finderCreate(extraIps: String?): Long
    /** Returns `[name0, url0, name1, url1, ...]`. Blocks up to [timeoutMs]. */
    external fun finderGetSources(finder: Long, timeoutMs: Int): Array<String>
    external fun finderDestroy(finder: Long)

    external fun receiverCreate(sourceName: String, urlAddress: String?, highBandwidth: Boolean, receiverName: String): Long
    external fun receiverDestroy(receiver: Long)
    external fun receiverSetSurface(receiver: Long, surface: Surface?)
    /** Returns `[connections, ptzSupported, width, height, frames]`. */
    external fun receiverStatus(receiver: Long): IntArray

    external fun ptzPanTiltSpeed(receiver: Long, pan: Float, tilt: Float): Boolean
    external fun ptzZoomSpeed(receiver: Long, speed: Float): Boolean
    external fun ptzFocusSpeed(receiver: Long, speed: Float): Boolean
    external fun ptzAutoFocus(receiver: Long): Boolean
    external fun ptzStorePreset(receiver: Long, index: Int): Boolean
    external fun ptzRecallPreset(receiver: Long, index: Int, speed: Float): Boolean
}
