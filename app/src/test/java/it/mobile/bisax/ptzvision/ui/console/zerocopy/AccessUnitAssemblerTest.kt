package it.mobile.bisax.ptzvision.ui.console.zerocopy

import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_AUD
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_IDR
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_PPS
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_SLICE
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_SPS
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.annexB
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.nal
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.packet
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessUnitAssemblerTest {

    private lateinit var listener: RecordingListener
    private lateinit var assembler: RtspNalExtractor.AccessUnitAssembler
    private var seq = 1000

    @Before
    fun setUp() {
        listener = RecordingListener()
        assembler = RtspNalExtractor.AccessUnitAssembler(96, listener)
    }

    private fun push(payload: ByteArray, ts: Long, marker: Boolean = true, pt: Int = 96) {
        val p = packet(seq++, ts, payload, marker, payloadType = pt)
        assembler.pushRtp(p, p.size)
    }

    private val idr = nal(NAL_IDR, 1, 2, 3)
    private val slice = nal(NAL_SLICE, 4, 5)

    @Test
    fun `single NAL with marker is emitted immediately as Annex-B`() {
        push(idr, ts = 3000)

        assertEquals(1, listener.accessUnits.size)
        val au = listener.accessUnits[0]
        assertTrue(au.key)
        assertArrayEquals(annexB(idr), au.data)
    }

    @Test
    fun `frames before the first keyframe are dropped`() {
        push(slice, ts = 3000)
        push(slice, ts = 6000)
        assertTrue(listener.accessUnits.isEmpty())

        push(idr, ts = 9000)
        push(slice, ts = 12000)
        assertEquals(listOf(true, false), listener.accessUnits.map { it.key })
    }

    @Test
    fun `slices of one picture are grouped into a single access unit`() {
        val slice2 = nal(NAL_IDR, 9, 9)
        push(idr, ts = 3000, marker = false)
        push(slice2, ts = 3000, marker = true)

        assertEquals(1, listener.accessUnits.size)
        assertArrayEquals(annexB(idr, slice2), listener.accessUnits[0].data)
    }

    @Test
    fun `timestamp change flushes a frame even without marker bit`() {
        push(idr, ts = 3000, marker = false)
        assertTrue(listener.accessUnits.isEmpty())

        push(slice, ts = 6000, marker = false)
        assertEquals(1, listener.accessUnits.size)
        assertArrayEquals(annexB(idr), listener.accessUnits[0].data)
    }

    @Test
    fun `FU-A fragments are reassembled with the original NAL header`() {
        val bigIdr = byteArrayOf(NAL_IDR.toByte()) + ByteArray(5000) { (it % 251).toByte() }
        val fragments = Rtp.fuA(bigIdr, chunk = 1400)
        assertEquals(4, fragments.size)

        fragments.forEachIndexed { i, f -> push(f, ts = 3000, marker = i == fragments.lastIndex) }

        assertEquals(1, listener.accessUnits.size)
        assertTrue(listener.accessUnits[0].key)
        assertArrayEquals(annexB(bigIdr), listener.accessUnits[0].data)
    }

    @Test
    fun `STAP-A is split into NALs and SPS-PPS are reported`() {
        val sps = nal(NAL_SPS, 0x42, 0xC0, 0x1F)
        val pps = nal(NAL_PPS, 0xCE, 0x3C)
        push(Rtp.stapA(sps, pps), ts = 3000, marker = false)
        push(idr, ts = 3000)

        assertEquals(1, listener.parameterSets.size)
        assertArrayEquals(annexB(sps), listener.parameterSets[0].first)
        assertArrayEquals(annexB(pps), listener.parameterSets[0].second)
        assertArrayEquals(annexB(sps, pps, idr), listener.accessUnits.single().data)
    }

    @Test
    fun `access unit delimiters are stripped`() {
        push(nal(NAL_AUD, 0xF0), ts = 3000, marker = false)
        push(idr, ts = 3000)

        assertArrayEquals(annexB(idr), listener.accessUnits.single().data)
    }

    @Test
    fun `packet loss drops frames until the next keyframe`() {
        push(idr, ts = 3000)
        seq++ // lost packet
        push(slice, ts = 6000)   // corrupt → dropped
        push(slice, ts = 9000)   // references a broken frame → dropped
        push(idr, ts = 12000)    // recovery
        push(slice, ts = 15000)

        assertEquals(listOf(true, true, false), listener.accessUnits.map { it.key })
    }

    @Test
    fun `FU-A continuation without start fragment is treated as corrupt`() {
        push(idr, ts = 3000)
        val fragments = Rtp.fuA(nal(NAL_SLICE, *IntArray(3000) { 7 }), chunk = 1400)
        // Lose the first fragment but keep sequence numbers contiguous (e.g. sender bug).
        fragments.drop(1).forEachIndexed { i, f -> push(f, ts = 6000, marker = i == fragments.size - 2) }
        push(slice, ts = 9000)

        assertEquals(1, listener.accessUnits.size) // only the first IDR survived
    }

    @Test
    fun `packets with another payload type are ignored`() {
        push(idr, ts = 3000, pt = 97)
        assertTrue(listener.accessUnits.isEmpty())
    }

    @Test
    fun `RTP header with CSRC, extension and padding is parsed`() {
        val p = packet(1, 3000, idr, marker = true, csrcCount = 2, extensionWords = 3, padding = 4)
        assembler.pushRtp(p, p.size)

        assertArrayEquals(annexB(idr), listener.accessUnits.single().data)
    }

    @Test
    fun `truncated and non RTP v2 packets are ignored`() {
        assembler.pushRtp(ByteArray(8), 8)
        val v1 = packet(1, 3000, idr).also { it[0] = 0x40 }
        assembler.pushRtp(v1, v1.size)
        assertTrue(listener.accessUnits.isEmpty())
    }

    @Test
    fun `only the first len bytes of the buffer are used`() {
        val p = packet(1, 3000, idr)
        val buffer = p + ByteArray(100) { 0x55 } // reused, larger network buffer
        assembler.pushRtp(buffer, p.size)

        assertArrayEquals(annexB(idr), listener.accessUnits.single().data)
        assertFalse(listener.accessUnits.single().data.contains(0x55))
    }
}
