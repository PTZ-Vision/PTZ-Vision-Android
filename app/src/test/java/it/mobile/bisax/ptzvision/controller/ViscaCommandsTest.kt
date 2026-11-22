package it.mobile.bisax.ptzvision.controller

import it.mobile.bisax.ptzvision.controller.utils.ByteParam
import it.mobile.bisax.ptzvision.controller.utils.HexConverter
import it.mobile.bisax.ptzvision.controller.utils.MathUtils
import it.mobile.bisax.ptzvision.controller.utils.MultiByteParam
import it.mobile.bisax.ptzvision.controller.viscacommands.AutoTracking
import it.mobile.bisax.ptzvision.controller.viscacommands.Focus
import it.mobile.bisax.ptzvision.controller.viscacommands.PanTilt
import it.mobile.bisax.ptzvision.controller.viscacommands.Scene
import it.mobile.bisax.ptzvision.controller.viscacommands.Zoom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Byte-exact checks of the VISCA commands sent to the cameras. */
class ViscaCommandsTest {

    private fun hex(bytes: ByteArray) = HexConverter.parseHex(bytes)

    // ------------------------------------------------------------ HexConverter

    @Test
    fun `hex round trip`() {
        val bytes = HexConverter.parseHex("8101FFa0")
        assertArrayEquals(byteArrayOf(0x81.toByte(), 0x01, 0xFF.toByte(), 0xA0.toByte()), bytes)
        assertEquals("8101FFA0", HexConverter.parseHex(bytes))
        assertEquals(0, HexConverter.parseHex("").size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `odd length hex is rejected`() {
        HexConverter.parseHex("810")
    }

    @Test(expected = NumberFormatException::class)
    fun `non hex digit is rejected`() {
        HexConverter.parseHex("8G")
    }

    // ----------------------------------------------------------------- params

    @Test
    fun `ByteParam accepts the range and rejects outside`() {
        val p = ByteParam("speed", 1, 24)
        assertEquals(1.toByte(), p.toByte(1))
        assertEquals(24.toByte(), p.toByte(24))
        assertThrows { p.toByte(0) }
        assertThrows { p.toByte(25) }
        assertThrows { ByteParam("bad", 5, 1) }
        assertThrows { ByteParam("bad", -1, 1) }
    }

    @Test
    fun `MultiByteParam splits value into VISCA nibbles`() {
        val p = MultiByteParam("zoom", 4, 0, 16384)
        assertArrayEquals(byteArrayOf(0x04, 0x00, 0x00, 0x00), p.toByte(0x4000))
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), p.toByte(0x1234))
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), p.toByte(0))
        assertThrows { p.toByte(16385) }
        assertThrows { MultiByteParam("bad", 5, 0, 1) }
    }

    @Test
    fun `MathUtils clamps`() {
        assertEquals(1f, MathUtils.clampUnit(3f))
        assertEquals(-1f, MathUtils.clampUnit(-3f))
        assertEquals(0.25f, MathUtils.clampUnit(0.25f))
        assertEquals(5f, MathUtils.clamp(10f, 0f, 5f))
    }

    // -------------------------------------------------------------- commands

    @Test
    fun `pan tilt commands`() {
        assertEquals("810106011814 0301FF".nospace(), hex(PanTilt.up(24, 20)))
        assertEquals("810106010A05 0302FF".nospace(), hex(PanTilt.down(10, 5)))
        assertEquals("810106010C14 0103FF".nospace(), hex(PanTilt.left(12)))
        assertEquals("810106010C14 0203FF".nospace(), hex(PanTilt.right(12)))
        assertEquals("810106010101 0101FF".nospace(), hex(PanTilt.upLeft(1, 1)))
        assertEquals("810106010101 0201FF".nospace(), hex(PanTilt.upRight(1, 1)))
        assertEquals("810106010101 0102FF".nospace(), hex(PanTilt.downLeft(1, 1)))
        assertEquals("810106010101 0202FF".nospace(), hex(PanTilt.downRight(1, 1)))
        assertEquals("810106011814 0303FF".nospace(), hex(PanTilt.stop()))
        assertEquals("81010604FF", hex(PanTilt.home()))
        assertEquals("81010605FF", hex(PanTilt.reset()))
        assertThrows { PanTilt.up(25, 1) }
        assertThrows { PanTilt.up(1, 21) }
    }

    @Test
    fun `zoom commands`() {
        assertEquals("8101040700FF", hex(Zoom.stop()))
        assertEquals("8101040702FF", hex(Zoom.tele()))
        assertEquals("8101040703FF", hex(Zoom.wide()))
        assertEquals("8101040725FF", hex(Zoom.tele(5)))
        assertEquals("8101040737FF", hex(Zoom.wide(7)))
        assertEquals("8101044704000000FF", hex(Zoom.direct(0x4000)))
        assertEquals("81090447FF", hex(Zoom.get()))
        assertThrows { Zoom.tele(8) }
    }

    @Test
    fun `focus commands`() {
        assertEquals("8101040802FF", hex(Focus.far()))
        assertEquals("8101040803FF", hex(Focus.near()))
        assertEquals("8101040823FF", hex(Focus.far(3)))
        assertEquals("8101040831FF", hex(Focus.near(1)))
        assertEquals("8101043802FF", hex(Focus.auto()))
        assertEquals("8101043803FF", hex(Focus.manual()))
        assertEquals("8101040800FF", hex(Focus.stop()))
    }

    @Test
    fun `scene and tracking commands`() {
        assertEquals("8101043F0103FF", hex(Scene.set(3)))
        assertEquals("8101043F0207FF", hex(Scene.call(7)))
        assertThrows { Scene.call(255) }
        assertEquals("810A115402FF", hex(AutoTracking.on()))
        assertEquals("810A115403FF", hex(AutoTracking.off()))
    }

    private fun String.nospace() = replace(" ", "")

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }
}
