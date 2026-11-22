package it.mobile.bisax.ptzvision.ui.console.zerocopy

import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_IDR
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.NAL_SLICE
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.annexB
import it.mobile.bisax.ptzvision.ui.console.zerocopy.Rtp.nal
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.util.Base64
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end test of the RTSP handshake and interleaved RTP parsing against a
 * fake RTSP server on localhost.
 */
class RtspNalExtractorSessionTest {

    private val server = ServerSocket(0)
    private val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var extractor: RtspNalExtractor? = null
    private val url get() = "rtsp://127.0.0.1:${server.localPort}/2"

    private val idr = nal(NAL_IDR, 1, 2, 3, 4)
    private val slice = nal(NAL_SLICE, 5, 6)

    @After
    fun tearDown() {
        extractor?.stop()
        server.close()
    }

    /** Waits for a condition the extractor thread fulfils. */
    private fun await(timeoutMs: Long = 3_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < end) { "Timed out. Requests: $requests" }
            Thread.sleep(10)
        }
    }

    /**
     * Minimal RTSP server: answers the handshake, then runs [afterPlay] with
     * the socket output to push interleaved data.
     */
    private fun serve(
        codec: String = "H264",
        describeStatus: String = "200 OK",
        afterPlay: (OutputStream) -> Unit = {},
    ): CountDownLatch {
        val done = CountDownLatch(1)
        Thread {
            server.accept().use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.US_ASCII))
                val out = s.getOutputStream()
                while (true) {
                    val lines = generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                    if (lines.isEmpty()) break
                    val request = lines.joinToString("\n")
                    requests += request
                    val method = lines[0].substringBefore(' ')
                    val cseq = lines.first { it.startsWith("CSeq") }.substringAfter(':').trim()
                    fun reply(status: String, headers: String = "", body: String = "") {
                        val len = if (body.isNotEmpty()) "Content-Length: ${body.length}\r\n" else ""
                        out.write("RTSP/1.0 $status\r\nCSeq: $cseq\r\n$headers$len\r\n$body".toByteArray())
                        out.flush()
                    }
                    when (method) {
                        "OPTIONS" -> reply("200 OK", "Public: DESCRIBE, SETUP, PLAY\r\n")
                        "DESCRIBE" -> {
                            if (!describeStatus.startsWith("200")) {
                                reply(describeStatus); break
                            }
                            val sdp = "v=0\r\nm=video 0 RTP/AVP 96\r\na=rtpmap:96 $codec/90000\r\n" +
                                "a=control:trackID=0\r\n"
                            reply("200 OK", "Content-Base: $url/\r\nContent-Type: application/sdp\r\n", sdp)
                        }
                        "SETUP" -> reply(
                            "200 OK",
                            "Session: 12345678;timeout=60\r\nTransport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n"
                        )
                        "PLAY" -> {
                            reply("200 OK", "Session: 12345678\r\n")
                            afterPlay(out)
                        }
                        "TEARDOWN" -> break
                    }
                }
            }
            done.countDown()
        }.apply { isDaemon = true; start() }
        return done
    }

    private fun OutputStream.interleaved(channel: Int, data: ByteArray) {
        write(byteArrayOf('$'.code.toByte(), channel.toByte(), (data.size shr 8).toByte(), data.size.toByte()))
        write(data)
        flush()
    }

    private fun start(listener: RecordingListener, user: String = "", pass: String = "") {
        extractor = RtspNalExtractor(url, user, pass, listener).also { it.start() }
    }

    @Test
    fun `full handshake delivers access units and ignores RTCP and RTSP replies`() {
        serve { out ->
            out.interleaved(0, Rtp.packet(1, 3000, idr))
            out.interleaved(1, ByteArray(28) { 0x7F }) // RTCP on channel 1
            // An interleaved RTSP response (e.g. keep-alive answer) in the middle of the media
            out.write("RTSP/1.0 200 OK\r\nCSeq: 99\r\nContent-Length: 4\r\n\r\nxxxx".toByteArray())
            out.interleaved(0, Rtp.packet(2, 6000, slice))
            out.flush()
        }
        val listener = RecordingListener()
        start(listener)

        await { listener.accessUnits.size == 2 }
        assertTrue(listener.connected)
        assertNull(listener.error)
        assertArrayEquals(annexB(idr), listener.accessUnits[0].data)
        assertArrayEquals(annexB(slice), listener.accessUnits[1].data)

        val methods = requests.map { it.substringBefore(' ') }
        assertEquals(listOf("OPTIONS", "DESCRIBE", "SETUP", "PLAY"), methods.take(4))
        val setup = requests.first { it.startsWith("SETUP") }
        assertTrue(setup, setup.startsWith("SETUP $url/trackID=0 RTSP/1.0"))
        assertTrue(setup, setup.contains("Transport: RTP/AVP/TCP;unicast;interleaved=0-1"))
        assertTrue(requests.first { it.startsWith("PLAY") }.contains("Session: 12345678"))
    }

    @Test
    fun `stop closes the connection without reporting an error`() {
        val done = serve { it.interleaved(0, Rtp.packet(1, 3000, idr)) }
        val listener = RecordingListener()
        start(listener)
        await { listener.accessUnits.isNotEmpty() }

        extractor!!.stop()
        assertTrue(done.await(3, TimeUnit.SECONDS))
        assertNull("stop() must not be reported as an error", listener.error)
    }

    @Test
    fun `basic auth header is sent when credentials are given`() {
        serve()
        val listener = RecordingListener()
        start(listener, user = "admin", pass = "secret")
        await { listener.connected }

        val expected = "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:secret".toByteArray())
        assertTrue(requests.all { it.contains(expected) })
    }

    @Test
    fun `DESCRIBE error is reported as a recoverable error`() {
        serve(describeStatus = "404 Not Found")
        val listener = RecordingListener()
        start(listener)

        await { listener.error != null }
        assertTrue(listener.error!!, listener.error!!.contains("404"))
        assertNull(listener.unsupported)
    }

    @Test
    fun `H265 stream is reported as unsupported for fallback`() {
        serve(codec = "H265")
        val listener = RecordingListener()
        start(listener)

        await { listener.unsupported != null }
        assertNotNull(listener.unsupported)
        assertNull(listener.error)
    }

    @Test
    fun `connection refused is reported as an error`() {
        val port = server.localPort
        server.close()
        val listener = RecordingListener()
        extractor = RtspNalExtractor("rtsp://127.0.0.1:$port/2", "", "", listener).also { it.start() }

        await { listener.error != null }
        assertTrue(!listener.connected)
    }
}
