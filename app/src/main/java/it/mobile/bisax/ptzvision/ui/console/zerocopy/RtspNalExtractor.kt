package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.util.Base64
import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RtspNalExtractor
 *
 * A minimal RTSP/TCP demuxer that extracts raw H.264 NAL units from an RTP stream
 * and delivers them to [onNalUnit] without any intermediate buffering.
 *
 * Supported:
 *  - RTSP over TCP (interleaved RTP, RFC 2326 Section 10.12)
 *  - H.264 RTP payload (RFC 6184): Single NAL, STAP-A, FU-A
 *  - Basic authentication (if username/password are supplied)
 */
class RtspNalExtractor(
    private val url: String,
    private val username: String,
    private val password: String,
    private val onNalUnit: (data: ByteArray, pts: Long) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "RtspNalExtractor"
        private val START_CODE = byteArrayOf(0, 0, 0, 1)
    }

    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    private var socket: Socket? = null

    fun start() {
        if (running.getAndSet(true)) return
        thread = Thread(::run, "RtspNalExtractor").also {
            it.isDaemon = true
            it.start()
        }
    }

    fun stop() {
        running.set(false)
        socket?.close()
        thread?.join(3_000)
    }

    private fun run() {
        try {
            connect()
        } catch (e: Exception) {
            Log.w(TAG, "Connection error: ${e.message}")
            onError("RTSP connection failed: ${e.message}")
        } finally {
            running.set(false)
        }
    }

    private fun connect() {
        val uri = URI.create(url)
        val host = uri.host
        val port = if (uri.port > 0) uri.port else 554

        Log.d(TAG, "Connecting to $host:$port")
        socket = Socket(host, port).also { s ->
            s.soTimeout = 5_000
            s.tcpNoDelay = true
            s.setPerformancePreferences(0, 1, 0)

            val input = BufferedInputStream(s.getInputStream())
            val output = s.getOutputStream()

            var cseq = 1
            val authLine = authorizationHeader()?.let { "Authorization: $it\r\n" } ?: ""

            fun send(request: String) {
                output.write(request.toByteArray(Charsets.US_ASCII))
                output.flush()
            }

            fun readResponse(): RtspResponse {
                val statusLine = readLine(input) ?: throw IOException("Missing RTSP status line")
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isBlank()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        headers[line.substring(0, colon).trim().lowercase()] =
                            line.substring(colon + 1).trim()
                    }
                }
                val length = headers["content-length"]?.toIntOrNull() ?: 0
                val body = if (length > 0) readFully(input, length) else ByteArray(0)
                return RtspResponse(statusLine, headers, body)
            }

            fun requireOk(resp: RtspResponse, stage: String) {
                val code = resp.statusCode()
                if (code != 200) {
                    throw IOException("$stage failed with RTSP $code: ${resp.statusLine}")
                }
            }

            // OPTIONS
            send("OPTIONS $url RTSP/1.0\r\nCSeq: ${cseq++}\r\n$authLine\r\n")
            requireOk(readResponse(), "OPTIONS")

            // DESCRIBE - extract SDP for SPS/PPS
            send(
                "DESCRIBE $url RTSP/1.0\r\n" +
                    "CSeq: ${cseq++}\r\n" +
                    "Accept: application/sdp\r\n" +
                    authLine +
                    "\r\n"
            )
            val describeResp = readResponse()
            requireOk(describeResp, "DESCRIBE")
            val sdp = String(describeResp.body)
            parseSdpSpsPps(sdp)
            val baseUrl = describeResp.headers["content-base"] ?: url

            // SETUP - request interleaved TCP (channel 0)
            val controlUrl = extractControlUrl(sdp, baseUrl)
            send(
                "SETUP $controlUrl RTSP/1.0\r\n" +
                    "CSeq: ${cseq++}\r\n" +
                    "Transport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n" +
                    authLine +
                    "\r\n"
            )
            val setupResp = readResponse()
            requireOk(setupResp, "SETUP")
            val session = setupResp.headers["session"]?.substringBefore(';').orEmpty()

            // PLAY - live stream
            send(
                "PLAY $url RTSP/1.0\r\n" +
                    "CSeq: ${cseq++}\r\n" +
                    "Session: $session\r\n" +
                    "Range: npt=0.000-\r\n" +
                    authLine +
                    "\r\n"
            )
            requireOk(readResponse(), "PLAY")

            Log.d(TAG, "PLAY started, session=$session")

            val rtpBuffer = RtpReassembler()
            while (running.get()) {
                val dollar = input.read()
                if (dollar == -1) throw IOException("RTSP socket closed")
                if (dollar != '$'.code) continue

                val channel = input.read()
                val length = (input.read() shl 8) or input.read()
                if (length <= 0) continue
                val payload = readFully(input, length)

                if (channel == 0 && length >= 12) {
                    val nalu = rtpBuffer.push(payload) ?: continue
                    val annexB = START_CODE + nalu
                    onNalUnit(annexB, rtpBuffer.lastPts)
                }
            }
        }
    }

    private fun parseSdpSpsPps(sdp: String) {
        val fmtp = sdp.lines().firstOrNull { it.startsWith("a=fmtp:") } ?: return
        val params = fmtp.substringAfter("sprop-parameter-sets=").split(",")
        if (params.size >= 2) {
            val sps = START_CODE + Base64.decode(params[0].trim(), Base64.NO_WRAP)
            val pps = START_CODE + Base64.decode(params[1].trim(), Base64.NO_WRAP)
            Log.d(TAG, "SPS/PPS from SDP: ${sps.size} / ${pps.size} bytes")
            onNalUnit(sps, 0L)
            onNalUnit(pps, 0L)
        }
    }

    private fun extractControlUrl(sdp: String, base: String): String {
        var inVideo = false
        var control: String? = null
        sdp.lines().forEach { line ->
            if (line.startsWith("m=")) {
                inVideo = line.startsWith("m=video")
            } else if (inVideo && line.startsWith("a=control:")) {
                control = line.removePrefix("a=control:").trim()
                return@forEach
            }
        }
        if (control == null) {
            control = sdp.lines()
                .firstOrNull { it.startsWith("a=control:") }
                ?.removePrefix("a=control:")
                ?.trim()
                ?: "trackID=0"
        }
        return if (control.startsWith("rtsp://")) control
        else "$base/$control"
    }

    private fun authorizationHeader(): String? {
        if (username.isBlank()) return null
        val creds = "$username:$password"
        val encoded = Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isNotEmpty()) sb.toString() else null
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }

    private fun readFully(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read == -1) throw IOException("Unexpected EOF")
            offset += read
        }
        return buffer
    }

    private data class RtspResponse(
        val statusLine: String,
        val headers: Map<String, String>,
        val body: ByteArray
    ) {
        fun statusCode(): Int? {
            val parts = statusLine.split(" ")
            return parts.getOrNull(1)?.toIntOrNull()
        }
    }

    private class RtpReassembler {
        var lastPts: Long = 0L
        private val fuBuffer = mutableListOf<ByteArray>()
        private var fuStarted = false

        fun push(rtp: ByteArray): ByteArray? {
            val timestamp =
                ((rtp[4].toInt() and 0xFF) shl 24) or
                    ((rtp[5].toInt() and 0xFF) shl 16) or
                    ((rtp[6].toInt() and 0xFF) shl 8) or
                    (rtp[7].toInt() and 0xFF)
            lastPts = timestamp * 1000L / 90

            if (rtp.size <= 12) return null
            val nalHeader = rtp[12].toInt() and 0xFF
            val nalType = nalHeader and 0x1F

            return when (nalType) {
                in 1..23 -> rtp.copyOfRange(12, rtp.size)
                24 -> if (rtp.size > 15) rtp.copyOfRange(15, rtp.size) else null
                28 -> {
                    val fuHeader = rtp[13].toInt() and 0xFF
                    val isStart = (fuHeader and 0x80) != 0
                    val isEnd = (fuHeader and 0x40) != 0
                    val fuNalType = fuHeader and 0x1F
                    val fragment = rtp.copyOfRange(14, rtp.size)

                    if (isStart) {
                        fuBuffer.clear()
                        fuStarted = true
                        val reconstructedHeader = ((nalHeader and 0xE0) or fuNalType).toByte()
                        fuBuffer.add(byteArrayOf(reconstructedHeader))
                    }
                    if (fuStarted) fuBuffer.add(fragment)
                    if (isEnd && fuStarted) {
                        fuStarted = false
                        fuBuffer.flatten().toByteArray()
                    } else null
                }
                else -> null
            }
        }

        private fun List<ByteArray>.flatten(): List<Byte> = flatMap { it.toList() }
    }
}

private fun List<Byte>.toByteArray() = ByteArray(size) { this[it] }
