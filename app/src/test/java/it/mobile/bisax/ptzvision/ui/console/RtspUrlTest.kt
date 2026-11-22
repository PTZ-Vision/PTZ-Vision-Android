package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.data.cam.Cam
import org.junit.Assert.assertEquals
import org.junit.Test

class RtspUrlTest {

    private fun cam(ip: String, streamPort: Int) = Cam(
        name = "test", ip = ip, controlPort = 5678, streamPort = streamPort, httpPort = 80, active = true
    )

    @Test
    fun `rtsp url uses ip, stream port and path 2`() {
        assertEquals("rtsp://192.168.1.11:8554/2", cam("192.168.1.11", 8554).rtspUrl())
    }

    @Test
    fun `PTZ state does not affect the url`() {
        // The previews are keyed on this url: moving the camera must not restart the stream.
        val a = cam("10.0.0.5", 554)
        val b = a.copy(pan = 0.5f, tilt = -0.2f, zoom = 3f, focus = 1f)
        assertEquals(a.rtspUrl(), b.rtspUrl())
    }
}
