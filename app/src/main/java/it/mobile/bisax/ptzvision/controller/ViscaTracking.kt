package it.mobile.bisax.ptzvision.controller

import android.util.Log
import it.mobile.bisax.ptzvision.controller.viscacommands.AutoTracking
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

/** PTZOptics G3 tracking extension, sent over raw VISCA/TCP on the control port. */
internal object ViscaTracking {
    fun set(host: String, port: Int, enabled: Boolean): PTZController.Result {
        if (port !in 1..65535) return PTZController.Result.FAILURE
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                socket.soTimeout = 7000
                socket.getOutputStream().write(if (enabled) AutoTracking.on() else AutoTracking.off())
                if (completed(socket.getInputStream())) PTZController.Result.SUCCESS else PTZController.Result.FAILURE
            }
        } catch (e: IOException) {
            Log.w("NdiTracking", "VISCA tracking command failed", e)
            PTZController.Result.FAILURE
        }
    }

    // A command may first receive an ACK (4x). Only completion (5x) confirms success.
    internal fun completed(input: InputStream): Boolean {
        var ackSocket = -1
        repeat(2) {
            if (input.read() != 0x90) return false
            val reply = input.read()
            if (reply < 0 || input.read() != 0xff) return false
            val socket = reply and 0x0f
            when (reply ushr 4) {
                4 -> ackSocket = socket
                5 -> return ackSocket == -1 || ackSocket == socket
                else -> return false
            }
        }
        return false
    }
}
