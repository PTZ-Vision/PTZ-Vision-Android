package it.mobile.bisax.ptzvision.ndi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NdiSourceTest {

    @Test
    fun `canonical name is split into machine and source`() {
        val s = NdiSource("BIRDDOG-1234 (CAM 1)", "192.168.1.20:5961")
        assertEquals("BIRDDOG-1234", s.machineName)
        assertEquals("CAM 1", s.sourceName)
        assertEquals("192.168.1.20", s.host)
    }

    @Test
    fun `nested parentheses keep the outer source part`() {
        assertEquals("PC" to "OBS (Program)", parseNdiName("PC (OBS (Program))"))
    }

    @Test
    fun `names without parentheses are returned unchanged`() {
        assertEquals("CAMERA" to "CAMERA", parseNdiName("CAMERA"))
    }

    @Test
    fun `host parsing handles ipv4, ipv6 and missing addresses`() {
        assertEquals("10.0.0.5", hostOf("10.0.0.5:5961"))
        assertEquals("10.0.0.5", hostOf("10.0.0.5"))
        assertEquals("fe80::1", hostOf("[fe80::1]:5961"))
        assertNull(hostOf("  "))
        assertNull(NdiSource("A (B)").host)
    }

    @Test
    fun `native finder output is decoded, cleaned and sorted`() {
        val decoded = decodeSources(arrayOf("ZED (2)", "10.0.0.2:5961", "", "x", "alpha (1)", ""))
        assertEquals(listOf("alpha (1)", "ZED (2)"), decoded.map { it.fullName })
        assertNull(decoded[0].address)
        assertEquals("10.0.0.2:5961", decoded[1].address)
    }
}
