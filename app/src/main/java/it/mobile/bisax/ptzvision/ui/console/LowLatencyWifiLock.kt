package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps Wi-Fi out of power-save while a live preview is on screen.
 *
 * With power save on, the access point buffers packets for the phone until
 * its next wake-up beacon: on a typical phone this shows up as 20-300 ms of
 * jitter and stalls that the zero-buffer pipeline cannot hide.
 * WIFI_MODE_FULL_LOW_LATENCY (API 29+) disables power save while the app is
 * in the foreground with the screen on.
 */
@Composable
fun LowLatencyWifiLockEffect() {
    val context = LocalContext.current.applicationContext
    DisposableEffect(Unit) {
        val lock = acquire(context)
        onDispose {
            try {
                if (lock?.isHeld == true) lock.release()
            } catch (e: RuntimeException) {
                Log.w("LowLatencyWifiLock", "release failed: ${e.message}")
            }
        }
    }
}

private fun acquire(context: Context): WifiManager.WifiLock? {
    val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val mode = WifiManager.WIFI_MODE_FULL_LOW_LATENCY
    return try {
        wifi.createWifiLock(mode, "PTZVision:preview").apply {
            setReferenceCounted(false)
            acquire()
        }.also { Log.d("LowLatencyWifiLock", "Wi-Fi lock acquired (mode=$mode)") }
    } catch (e: RuntimeException) {
        Log.w("LowLatencyWifiLock", "Unable to acquire Wi-Fi lock: ${e.message}")
        null
    }
}
