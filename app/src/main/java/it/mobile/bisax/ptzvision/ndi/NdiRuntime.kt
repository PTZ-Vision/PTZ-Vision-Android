package it.mobile.bisax.ptzvision.ndi

import android.content.Context
import android.net.nsd.NsdManager
import android.util.Log

/**
 * Process-wide NDI® runtime state.
 *
 * The NDI SDK on Android relies on the system mDNS daemon, which only runs while some component
 * holds an [NsdManager] instance: we keep one for the whole process lifetime, as required by the
 * SDK documentation ("Platform considerations > Android").
 */
object NdiRuntime {
    private const val TAG = "NdiRuntime"

    @Volatile
    private var nsdManager: NsdManager? = null

    @Volatile
    private var initialised = false

    /** True when libndi.so is loaded and initialised: PTZ control and NDI video are available. */
    @Volatile
    var isAvailable: Boolean = false
        private set

    /** Why NDI is not available (null when it is). */
    @Volatile
    var unavailableReason: String? = null
        private set

    /** NDI runtime version string, when available. */
    @Volatile
    var version: String? = null
        private set

    /** Receive NDI video at full bandwidth/resolution instead of the low-bandwidth proxy stream. */
    @Volatile
    var highBandwidthVideo: Boolean = false

    /** Idempotent; safe to call from any thread. Returns [isAvailable]. */
    fun init(context: Context): Boolean {
        if (initialised) return isAvailable
        synchronized(this) {
            if (initialised) return isAvailable
            nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager?

            if (!NdiNative.bridgeLoaded) {
                unavailableReason = "NDI bridge library not packaged"
            } else {
                val error = NdiNative.nativeInit()
                if (error == null) {
                    isAvailable = true
                    version = NdiNative.nativeVersion()
                    Log.i(TAG, "NDI available: $version")
                } else {
                    unavailableReason = error
                    Log.w(TAG, "NDI unavailable: $error")
                }
            }
            initialised = true
            return isAvailable
        }
    }

    internal fun nsd(): NsdManager? = nsdManager
}
