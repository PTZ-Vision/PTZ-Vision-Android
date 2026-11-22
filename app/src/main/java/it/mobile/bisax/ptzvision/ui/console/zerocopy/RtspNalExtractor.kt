package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal, zero-buffer RTSP/TCP client that turns an H.264 RTP stream into
 * complete Annex-B access units (one decoded picture each).
 *
 * Latency-relevant design choices:
 *  - RTP over TCP interleaved: no UDP probing / fallback timers.
 *  - No jitter buffer: an access unit is emitted the moment its last packet
 *    (RTP marker bit) arrives, so the decoder can output the picture
 *    immediately instead of waiting for the start of the next frame.
 *  - FU-A reassembly into a reusable ByteArrayOutputStream (no boxing).
 *
 * Supported: RFC 6184 single NAL, STAP-A, FU-A; Basic auth; keep-alive.
 * Unsupported codecs (e.g. H.265) are reported through [Listener.onUnsupported]
 * so the caller can fall back to the full-featured library player.
 */
class RtspNalExtractor(
    private val url: String,
    private val username: String,
    private val password: String,
    private val listener: Listener
) {
    interface Listener {
        /** SPS and PPS (Annex-B, with start code) known from SDP or in-band. */
        fun onParameterSets(sps: ByteArray, pps: ByteArray)

        /** One complete access unit, Annex-B formatted. */
        fun onAccessUnit(data: ByteArray, size: Int, isKeyFrame: Boolean)

        fun onConnected()
        fun onUnsupported(message: String)
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "RtspNalExtractor"
        private val START_CODE = byteArrayOf(0, 0, 0, 1)
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val DEFAULT_SESSION_TIMEOUT_S = 60

        /** Parses the first video track of an SDP. Throws [UnsupportedStreamException] for non-H.264. */
        internal fun parseSdp(sdp: String): SdpVideo {
            var inVideo = false
            var beforeMedia = true
            var payloadType = -1
            var control: String? = null
            var sessionControl: String? = null
            var encoding: String? = null
            var sprop: String? = null

            for (raw in sdp.lineSequence()) {
                val line = raw.trim()
                if (line.startsWith("m=")) {
                    if (inVideo) break // only first video track
                    beforeMedia = false
                    inVideo = line.startsWith("m=video")
                    if (inVideo) payloadType = line.split(' ').getOrNull(3)?.toIntOrNull() ?: 96
                    continue
                }
                if (!inVideo) {
                    if (beforeMedia && line.startsWith("a=control:")) {
                        sessionControl = line.removePrefix("a=control:").trim()
                    }
                    continue
                }
                when {
                    line.startsWith("a=control:") -> control = line.removePrefix("a=control:").trim()
                    line.startsWith("a=rtpmap:$payloadType ") ->
                        encoding = line.substringAfter(' ').substringBefore('/').trim().uppercase()
                    line.startsWith("a=fmtp:$payloadType ") ->
                        sprop = line.substringAfter(' ').split(';')
                            .map { it.trim() }
                            .firstOrNull { it.startsWith("sprop-parameter-sets=") }
                            ?.substringAfter('=')
                }
            }

            if (payloadType < 0) throw UnsupportedStreamException("No video track in SDP")
            if (encoding != null && encoding != "H264") {
                throw UnsupportedStreamException("Codec $encoding not supported by zero-copy path")
            }

            var sps: ByteArray? = null
            var pps: ByteArray? = null
            sprop?.split(',')?.forEach { b64 ->
                val nal = try {
                    Base64.getMimeDecoder().decode(b64.trim())
                } catch (_: IllegalArgumentException) {
                    return@forEach
                }
                if (nal.isEmpty()) return@forEach
                when (nal[0].toInt() and 0x1F) {
                    7 -> sps = START_CODE + nal
                    8 -> pps = START_CODE + nal
                }
            }
            return SdpVideo(payloadType, control ?: "*", sessionControl, sps, pps)
        }

        /** Resolves an SDP a=control attribute against the Content-Base URL. */
        internal fun resolveControl(base: String, control: String): String = when {
            control == "*" || control.isEmpty() -> base.trimEnd('/')
            control.startsWith("rtsp://", ignoreCase = true) -> control
            base.endsWith("/") -> base + control
            else -> "$base/$control"
        }
    }

    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    @Volatile private var socket: Socket? = null

    fun start() {
        if (running.getAndSet(true)) return
        thread = Thread(::run, "RtspNalExtractor").also {
            it.isDaemon = true
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun stop() {
        running.set(false)
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        val t = thread
        if (t != null && t != Thread.currentThread()) t.join(2_000)
        thread = null
    }

    private fun run() {
        try {
            session()
        } catch (e: UnsupportedStreamException) {
            if (running.get()) listener.onUnsupported(e.message ?: "Unsupported stream")
        } catch (e: Exception) {
            if (running.get()) {
                Log.w(TAG, "RTSP error: ${e.message}")
                listener.onError(e.message ?: e.javaClass.simpleName)
            }
        } finally {
            running.set(false)
            try {
                socket?.close()
            } catch (_: IOException) {
            }
        }
    }

    internal class UnsupportedStreamException(message: String) : Exception(message)

    private class RtspResponse(
        val code: Int,
        val statusLine: String,
        val headers: Map<String, String>,
        val body: ByteArray
    )

    // ------------------------------------------------------------------ RTSP

    private fun session() {
        val uri = URI.create(url)
        val host = uri.host ?: throw IOException("Invalid RTSP url: $url")
        val port = if (uri.port > 0) uri.port else 554
        // Never send credentials embedded in the URL on the request line.
        val requestUrl = URI(uri.scheme, null, uri.host, port, uri.path, uri.query, null).toString()

        Log.d(TAG, "Connecting to $host:$port")
        val s = Socket()
        socket = s
        s.tcpNoDelay = true
        s.receiveBufferSize = 512 * 1024
        s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        s.soTimeout = READ_TIMEOUT_MS

        val input = BufferedInputStream(s.getInputStream(), 64 * 1024)
        val output = s.getOutputStream()
        var cseq = 1
        val auth = authorizationHeader()?.let { "Authorization: $it\r\n" } ?: ""

        fun request(method: String, target: String, extra: String = ""): RtspResponse {
            send(output, "$method $target RTSP/1.0\r\nCSeq: ${cseq++}\r\nUser-Agent: PTZ-Vision\r\n$auth$extra\r\n")
            val resp = readResponse(input)
            if (resp.code == 401) throw IOException("$method: unauthorized (401)")
            if (resp.code != 200) throw IOException("$method failed: ${resp.statusLine}")
            return resp
        }

        request("OPTIONS", requestUrl)

        val describe = request("DESCRIBE", requestUrl, "Accept: application/sdp\r\n")
        val sdp = String(describe.body, Charsets.UTF_8)
        val video = parseSdp(sdp)
        val baseUrl = describe.headers["content-base"]
            ?: describe.headers["content-location"]
            ?: requestUrl
        val controlUrl = resolveControl(baseUrl, video.control)

        video.sps?.let { sps -> video.pps?.let { pps -> listener.onParameterSets(sps, pps) } }

        val setup = request(
            "SETUP", controlUrl,
            "Transport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n"
        )
        val sessionHeader = setup.headers["session"].orEmpty()
        val sessionId = sessionHeader.substringBefore(';').trim()
        val sessionTimeoutS = Regex("timeout=(\\d+)").find(sessionHeader)
            ?.groupValues?.get(1)?.toIntOrNull() ?: DEFAULT_SESSION_TIMEOUT_S
        val rtpChannel = Regex("interleaved=(\\d+)")
            .find(setup.headers["transport"].orEmpty())
            ?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val playUrl = resolveControl(baseUrl, video.sessionControl ?: "*")
        request("PLAY", playUrl, "Session: $sessionId\r\nRange: npt=0.000-\r\n")
        Log.d(TAG, "PLAY ok, session=$sessionId timeout=${sessionTimeoutS}s pt=${video.payloadType}")
        listener.onConnected()

        // ---- media loop
        val keepAliveIntervalNs = (sessionTimeoutS.coerceAtLeast(10) * 1_000_000_000L) / 2
        var lastKeepAlive = System.nanoTime()
        val assembler = AccessUnitAssembler(video.payloadType, listener)
        val header = ByteArray(3)
        var packet = ByteArray(64 * 1024)

        while (running.get()) {
            val now = System.nanoTime()
            if (now - lastKeepAlive > keepAliveIntervalNs) {
                send(output, "GET_PARAMETER $playUrl RTSP/1.0\r\nCSeq: ${cseq++}\r\nSession: $sessionId\r\n$auth\r\n")
                lastKeepAlive = now
            }

            val first = try {
                input.read()
            } catch (_: SocketTimeoutException) {
                throw IOException("No data from camera for ${READ_TIMEOUT_MS}ms")
            }
            when (first) {
                -1 -> throw IOException("Connection closed by camera")
                '$'.code -> {
                    readFully(input, header, 0, 3)
                    val channel = header[0].toInt() and 0xFF
                    val length = ((header[1].toInt() and 0xFF) shl 8) or (header[2].toInt() and 0xFF)
                    if (length > packet.size) packet = ByteArray(length)
                    readFully(input, packet, 0, length)
                    if (channel == rtpChannel) assembler.pushRtp(packet, length)
                }
                'R'.code -> {
                    // Interleaved RTSP response (keep-alive answer): consume it.
                    readResponse(input, alreadyRead = "R")
                }
                else -> Unit // resync: skip stray byte
            }
        }

        try {
            send(output, "TEARDOWN $playUrl RTSP/1.0\r\nCSeq: ${cseq++}\r\nSession: $sessionId\r\n$auth\r\n")
        } catch (_: IOException) {
        }
    }

    internal class SdpVideo(
        val payloadType: Int,
        val control: String,
        val sessionControl: String?,
        val sps: ByteArray?,
        val pps: ByteArray?
    )

    private fun authorizationHeader(): String? {
        if (username.isBlank()) return null
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return "Basic $encoded"
    }

    // --------------------------------------------------------------- I/O utils

    private fun send(output: OutputStream, request: String) {
        synchronized(output) {
            output.write(request.toByteArray(Charsets.US_ASCII))
            output.flush()
        }
    }

    private fun readResponse(input: InputStream, alreadyRead: String = ""): RtspResponse {
        val statusLine = alreadyRead + (readLine(input) ?: throw IOException("Connection closed"))
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
            }
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        val body = ByteArray(length)
        if (length > 0) readFully(input, body, 0, length)
        val code = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: -1
        return RtspResponse(code, statusLine, headers, body)
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

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int) {
        var pos = 0
        while (pos < length) {
            val read = input.read(buffer, offset + pos, length - pos)
            if (read == -1) throw IOException("Unexpected EOF")
            pos += read
        }
    }

    // ------------------------------------------------------------ RTP → AU

    /**
     * Collects RTP payloads into one Annex-B access unit and emits it on the
     * marker bit (or on timestamp change, for senders that don't set it).
     */
    internal class AccessUnitAssembler(
        private val payloadType: Int,
        private val listener: Listener
    ) {
        private val au = ExposedByteArrayOutputStream(256 * 1024)
        private var auTimestamp = 0L
        private var auHasData = false
        private var auKey = false
        private var auCorrupt = false
        private var fuActive = false
        private var lastSeq = -1
        private var waitingForKeyFrame = true

        private var sps: ByteArray? = null
        private var pps: ByteArray? = null

        fun pushRtp(p: ByteArray, len: Int) {
            if (len < 12) return
            val version = (p[0].toInt() and 0xC0) shr 6
            if (version != 2) return
            val padding = (p[0].toInt() and 0x20) != 0
            val extension = (p[0].toInt() and 0x10) != 0
            val csrcCount = p[0].toInt() and 0x0F
            val marker = (p[1].toInt() and 0x80) != 0
            val pt = p[1].toInt() and 0x7F
            if (pt != payloadType) return
            val seq = ((p[2].toInt() and 0xFF) shl 8) or (p[3].toInt() and 0xFF)
            val timestamp = ((p[4].toLong() and 0xFF) shl 24) or ((p[5].toLong() and 0xFF) shl 16) or
                ((p[6].toLong() and 0xFF) shl 8) or (p[7].toLong() and 0xFF)

            var offset = 12 + csrcCount * 4
            if (extension) {
                if (offset + 4 > len) return
                val extLen = ((p[offset + 2].toInt() and 0xFF) shl 8) or (p[offset + 3].toInt() and 0xFF)
                offset += 4 + extLen * 4
            }
            var end = len
            if (padding) end -= p[len - 1].toInt() and 0xFF
            if (offset >= end) return

            if (lastSeq >= 0 && seq != ((lastSeq + 1) and 0xFFFF)) {
                Log.w(TAG, "RTP packet loss: expected ${(lastSeq + 1) and 0xFFFF}, got $seq")
                auCorrupt = true
            }
            lastSeq = seq

            if (auHasData && timestamp != auTimestamp) flush()
            auTimestamp = timestamp

            val nalHeader = p[offset].toInt() and 0xFF
            when (val type = nalHeader and 0x1F) {
                in 1..23 -> appendNal(p, offset, end - offset, type)
                24 -> { // STAP-A
                    var pos = offset + 1
                    while (pos + 2 <= end) {
                        val size = ((p[pos].toInt() and 0xFF) shl 8) or (p[pos + 1].toInt() and 0xFF)
                        pos += 2
                        if (size == 0 || pos + size > end) break
                        appendNal(p, pos, size, p[pos].toInt() and 0x1F)
                        pos += size
                    }
                }
                28 -> { // FU-A
                    if (offset + 2 > end) return
                    val fu = p[offset + 1].toInt() and 0xFF
                    val start = (fu and 0x80) != 0
                    val fuType = fu and 0x1F
                    if (start) {
                        au.write(START_CODE)
                        au.write((nalHeader and 0xE0) or fuType)
                        fuActive = true
                        auHasData = true
                        if (fuType == 5) auKey = true
                    } else if (!fuActive) {
                        auCorrupt = true
                        return
                    }
                    au.write(p, offset + 2, end - offset - 2)
                    if ((fu and 0x40) != 0) fuActive = false
                }
                else -> Unit
            }

            if (marker) flush()
        }

        private fun appendNal(p: ByteArray, off: Int, size: Int, type: Int) {
            when (type) {
                7 -> sps = START_CODE + p.copyOfRange(off, off + size)
                8 -> {
                    pps = START_CODE + p.copyOfRange(off, off + size)
                    val s = sps
                    val q = pps
                    if (s != null && q != null) listener.onParameterSets(s, q)
                }
                5 -> auKey = true
                9 -> return // access unit delimiter: useless for MediaCodec
            }
            au.write(START_CODE)
            au.write(p, off, size)
            auHasData = true
        }

        private fun flush() {
            if (auHasData) {
                if (auCorrupt) {
                    // A broken reference frame would smear until the next IDR anyway;
                    // dropping is cheaper than decoding garbage.
                    waitingForKeyFrame = true
                } else {
                    if (auKey) waitingForKeyFrame = false
                    // The listener must consume the buffer synchronously (it is reused).
                    if (!waitingForKeyFrame) listener.onAccessUnit(au.array(), au.size(), auKey)
                }
            }
            au.reset()
            auHasData = false
            auKey = false
            auCorrupt = false
            fuActive = false
        }
    }

    /** Gives access to the internal buffer so an AU can be handed over without a copy. */
    private class ExposedByteArrayOutputStream(size: Int) : ByteArrayOutputStream(size) {
        fun array(): ByteArray = buf
    }
}
