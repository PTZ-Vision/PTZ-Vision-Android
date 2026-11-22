package it.mobile.bisax.ptzvision.ui.console.zerocopy

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class SdpParserTest {

    private val sps = byteArrayOf(0x67, 0x42, 0xC0.toByte(), 0x1F, 0xDA.toByte())
    private val pps = byteArrayOf(0x68, 0xCE.toByte(), 0x3C, 0x80.toByte())
    private val b64 = Base64.getEncoder()
    private val sprop = "${b64.encodeToString(sps)},${b64.encodeToString(pps)}"

    private fun h264Sdp(extra: String = "") = """
        v=0
        o=- 0 0 IN IP4 127.0.0.1
        s=Stream
        a=control:*
        $extra
        m=video 0 RTP/AVP 96
        a=rtpmap:96 H264/90000
        a=fmtp:96 packetization-mode=1; profile-level-id=42C01F; sprop-parameter-sets=$sprop
        a=control:trackID=0
    """.trimIndent().replace("\n", "\r\n")

    @Test
    fun `parses payload type, control and parameter sets`() {
        val video = RtspNalExtractor.parseSdp(h264Sdp())

        assertEquals(96, video.payloadType)
        assertEquals("trackID=0", video.control)
        assertEquals("*", video.sessionControl)
        assertArrayEquals(Rtp.START_CODE + sps, video.sps)
        assertArrayEquals(Rtp.START_CODE + pps, video.pps)
    }

    @Test
    fun `mediamtx style SDP without sprop still parses`() {
        val sdp = """
            v=0
            m=video 0 RTP/AVP 96
            a=control:rtsp://192.168.1.11:8554/2/trackID=0
            a=rtpmap:96 H264/90000
            a=fmtp:96 packetization-mode=1
        """.trimIndent()
        val video = RtspNalExtractor.parseSdp(sdp)

        assertEquals("rtsp://192.168.1.11:8554/2/trackID=0", video.control)
        assertNull(video.sps)
        assertNull(video.pps)
        assertNull(video.sessionControl)
    }

    @Test
    fun `audio track before video is skipped`() {
        val sdp = """
            v=0
            m=audio 0 RTP/AVP 97
            a=rtpmap:97 MPEG4-GENERIC/48000/2
            a=control:trackID=1
            m=video 0 RTP/AVP 98
            a=rtpmap:98 H264/90000
            a=control:trackID=2
        """.trimIndent()
        val video = RtspNalExtractor.parseSdp(sdp)

        assertEquals(98, video.payloadType)
        assertEquals("trackID=2", video.control)
        assertNull("audio control must not leak into session control", video.sessionControl)
    }

    @Test
    fun `missing control defaults to aggregate`() {
        val sdp = "v=0\r\nm=video 0 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n"
        assertEquals("*", RtspNalExtractor.parseSdp(sdp).control)
    }

    @Test(expected = RtspNalExtractor.UnsupportedStreamException::class)
    fun `H265 is reported as unsupported`() {
        RtspNalExtractor.parseSdp("v=0\nm=video 0 RTP/AVP 96\na=rtpmap:96 H265/90000\n")
    }

    @Test(expected = RtspNalExtractor.UnsupportedStreamException::class)
    fun `SDP without video is reported as unsupported`() {
        RtspNalExtractor.parseSdp("v=0\nm=audio 0 RTP/AVP 0\na=rtpmap:0 PCMU/8000\n")
    }

    @Test
    fun `resolveControl handles relative, absolute and aggregate controls`() {
        val base = "rtsp://cam:554/stream"
        assertEquals("rtsp://cam:554/stream/trackID=0", RtspNalExtractor.resolveControl(base, "trackID=0"))
        assertEquals("rtsp://cam:554/stream/trackID=0", RtspNalExtractor.resolveControl("$base/", "trackID=0"))
        assertEquals("rtsp://other/x", RtspNalExtractor.resolveControl(base, "rtsp://other/x"))
        assertEquals("RTSP://other/x", RtspNalExtractor.resolveControl(base, "RTSP://other/x"))
        assertEquals(base, RtspNalExtractor.resolveControl("$base/", "*"))
        assertEquals(base, RtspNalExtractor.resolveControl(base, ""))
    }
}
