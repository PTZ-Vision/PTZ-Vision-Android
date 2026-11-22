package it.mobile.bisax.ptzvision.ndi

import android.util.Log
import android.view.Surface
import java.util.concurrent.Executors

/** Snapshot of a receiver's state. */
data class NdiReceiverStatus(
    val connections: Int = 0,
    val ptzSupported: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val frames: Int = 0,
) {
    val connected: Boolean get() = connections > 0
}

/** A reference-counted lease on a native NDI receiver. Call [release] exactly once. */
class NdiReceiver internal constructor(
    internal val key: String,
    internal val handle: Long,
) {
    @Volatile
    private var released = false

    val isReleased: Boolean get() = released

    fun status(): NdiReceiverStatus {
        if (released) return NdiReceiverStatus()
        val s = NdiNative.receiverStatus(handle)
        return NdiReceiverStatus(s[0], s[1] != 0, s[2], s[3], s[4])
    }

    fun setSurface(surface: Surface?) {
        if (!released) NdiNative.receiverSetSurface(handle, surface)
    }

    fun release() {
        if (released) return
        released = true
        NdiReceiverPool.release(this)
    }
}

/**
 * Shares one native receiver per NDI source between the PTZ controller and the video preview,
 * so the camera sees a single connection from this device.
 */
object NdiReceiverPool {
    private const val TAG = "NdiReceiverPool"
    private const val RECEIVER_NAME = "PTZ Vision"

    private class Entry(val handle: Long, var refs: Int)

    private val entries = HashMap<String, Entry>()
    private val destroyer = Executors.newSingleThreadExecutor { r -> Thread(r, "ndi-recv-destroy") }

    /** Returns a lease on the receiver for [sourceName], creating it if needed; null if NDI is unavailable. */
    @Synchronized
    fun acquire(sourceName: String): NdiReceiver? {
        if (!NdiRuntime.isAvailable || sourceName.isBlank()) return null
        val key = sourceName
        val entry = entries[key] ?: run {
            // Connect by name: the SDK resolves the current address through mDNS, which is more
            // robust than a cached ip:port (cameras often get their address via DHCP).
            val handle = NdiNative.receiverCreate(sourceName, null, NdiRuntime.highBandwidthVideo, RECEIVER_NAME)
            if (handle == 0L) return null
            Entry(handle, 0).also { entries[key] = it }
        }
        entry.refs++
        return NdiReceiver(key, entry.handle)
    }

    @Synchronized
    internal fun release(receiver: NdiReceiver) {
        val entry = entries[receiver.key] ?: return
        if (entry.handle != receiver.handle) return
        entry.refs--
        if (entry.refs <= 0) {
            entries.remove(receiver.key)
            // Destroying joins the capture thread: keep it off the caller (often the main thread).
            destroyer.execute {
                NdiNative.receiverDestroy(entry.handle)
                Log.d(TAG, "Receiver for '${receiver.key}' destroyed")
            }
        }
    }
}
