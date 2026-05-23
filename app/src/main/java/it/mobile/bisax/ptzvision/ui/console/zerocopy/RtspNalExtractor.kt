package it.mobile.bisax.ptzvision.ui.console.zerocopy

import android.net.Uri
import android.util.Base64
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RtspNalExtractor(
    private val rtspUrl: String,
    private val scope: CoroutineScope,
    private val onSessionReady: (RtspSessionInfo) -> Unit,
    private val onNalUnit: (ByteArray) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private var job: Job? = null
    private var socket: Socket? = null
    private var fuBuffer: ByteArrayOutputStream? = null

    fun start() {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            runCatching { stream() }.onFailure { throwable ->
                if (throwable !is CancellationException) {
                    onError(throwable)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        closeSocket()
    }

    private fun stream() {
        val uri = Uri.parse(rtspUrl)
        val host = uri.host ?: throw IOException("Missing RTSP host")
        val port = if (uri.port == -1) DEFAULT_RTSP_PORT else uri.port
        val path = buildString {
            append(uri.encodedPath ?: "/")
            if (!uri.encodedQuery.isNullOrEmpty()) {
                append('?').append(uri.encodedQuery)
            }
        }
        val baseUrl = "rtsp://$host:$port$path"

        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        socket.tcpNoDelay = true
        this.socket = socket

        val input = BufferedInputStream(socket.getInputStream())
        val output = BufferedOutputStream(socket.getOutputStream())

        var cSeq = 1
        sendRequest(output, input, "OPTIONS", baseUrl, cSeq++)

        val describeResponse = sendRequest(output, input, "DESCRIBE", baseUrl, cSeq++) {
            append("Accept: application/sdp\r\n")
        }
        val sdp = describeResponse.body
        val sdpInfo = parseSdp(baseUrl, sdp)

        val setupUrl = sdpInfo.controlUrl
        val setupResponse = sendRequest(output, input, "SETUP", setupUrl, cSeq++) {
            append("Transport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n")
        }
        val sessionId = setupResponse.headers["session"]?.substringBefore(';')
            ?: throw IOException("Missing RTSP session header")

        sendRequest(output, input, "PLAY", baseUrl, cSeq++) {
            append("Session: $sessionId\r\n")
        }

        onSessionReady(sdpInfo)

        readInterleavedStream(input)
    }

    private fun sendRequest(
        output: BufferedOutputStream,
        input: BufferedInputStream,
        method: String,
        url: String,
        cSeq: Int,
        extraHeaders: StringBuilder.() -> Unit = {}
    ): RtspResponse {
        val builder = StringBuilder()
        builder.append("$method $url RTSP/1.0\r\n")
        builder.append("CSeq: $cSeq\r\n")
        builder.extraHeaders()
        builder.append("\r\n")

        output.write(builder.toString().toByteArray(StandardCharsets.UTF_8))
        output.flush()

        return readResponse(input)
    }

    private fun readResponse(input: InputStream): RtspResponse {
        val statusLine = readLine(input) ?: throw IOException("Missing RTSP status line")
        val statusParts = statusLine.split(" ")
        val statusCode = statusParts.getOrNull(1)?.toIntOrNull()
            ?: throw IOException("Invalid RTSP status line: $statusLine")

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: throw IOException("Unexpected EOF in headers")
            if (line.isEmpty()) break
            val headerParts = line.split(":", limit = 2)
            if (headerParts.size == 2) {
                headers[headerParts[0].trim().lowercase()] = headerParts[1].trim()
            }
        }

        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body = if (contentLength > 0) {
            val bodyBytes = readExactBytes(input, contentLength)
            String(bodyBytes, StandardCharsets.UTF_8)
        } else {
            ""
        }

        if (statusCode >= 300) {
            throw IOException("RTSP error $statusCode")
        }

        return RtspResponse(statusCode, headers, body)
    }

    private fun readLine(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val byte = input.read()
            if (byte == -1) {
                return if (buffer.size() == 0) null else buffer.toString(StandardCharsets.UTF_8.name())
            }
            if (byte == '\n'.code) {
                break
            }
            if (byte != '\r'.code) {
                buffer.write(byte)
            }
        }
        return buffer.toString(StandardCharsets.UTF_8.name())
    }

    private fun readInterleavedStream(input: InputStream) {
        while (job?.isActive == true) {
            val firstByte = input.read()
            if (firstByte == -1) {
                throw IOException("RTSP stream closed")
            }
            if (firstByte == INTERLEAVED_MAGIC) {
                val channel = input.read()
                val lengthHigh = input.read()
                val lengthLow = input.read()
                if (lengthHigh == -1 || lengthLow == -1) {
                    throw IOException("RTSP stream closed")
                }
                val length = (lengthHigh shl 8) or lengthLow
                if (length <= 0) continue
                val payload = readExactBytes(input, length)
                if (channel == RTP_CHANNEL) {
                    handleRtpPacket(payload)
                }
            } else {
                val lineBuffer = ByteArrayOutputStream()
                lineBuffer.write(firstByte)
                while (true) {
                    val byte = input.read()
                    if (byte == -1) break
                    lineBuffer.write(byte)
                    if (byte == '\n'.code) break
                }
                // Skip remaining RTSP headers
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                }
            }
        }
    }

    private fun handleRtpPacket(packet: ByteArray) {
        if (packet.size < RTP_HEADER_SIZE) return
        val version = packet[0].toInt() shr 6
        if (version != 2) return

        val cc = packet[0].toInt() and 0x0F
        val hasExtension = packet[0].toInt() and 0x10 != 0
        val hasPadding = packet[0].toInt() and 0x20 != 0

        var headerSize = RTP_HEADER_SIZE + cc * 4
        if (hasExtension) {
            if (packet.size < headerSize + 4) return
            val extensionLength = ((packet[headerSize + 2].toInt() and 0xFF) shl 8) or
                (packet[headerSize + 3].toInt() and 0xFF)
            headerSize += 4 + extensionLength * 4
        }

        if (headerSize >= packet.size) return

        var payloadEnd = packet.size
        if (hasPadding) {
            val padding = packet.last().toInt() and 0xFF
            payloadEnd = (payloadEnd - padding).coerceAtLeast(headerSize)
        }

        val payload = packet.copyOfRange(headerSize, payloadEnd)
        if (payload.isNotEmpty()) {
            handleNalPayload(payload)
        }
    }

    private fun handleNalPayload(payload: ByteArray) {
        val nalType = payload[0].toInt() and 0x1F
        when (nalType) {
            in 1..23 -> emitNal(payload)
            NAL_TYPE_STAP_A -> handleStapA(payload)
            NAL_TYPE_FU_A -> handleFuA(payload)
        }
    }

    private fun handleStapA(payload: ByteArray) {
        var offset = 1
        while (offset + 2 <= payload.size) {
            val nalLength = ((payload[offset].toInt() and 0xFF) shl 8) or
                (payload[offset + 1].toInt() and 0xFF)
            offset += 2
            if (offset + nalLength > payload.size) break
            val nal = payload.copyOfRange(offset, offset + nalLength)
            emitNal(nal)
            offset += nalLength
        }
    }

    private fun handleFuA(payload: ByteArray) {
        if (payload.size < 2) return
        val fuIndicator = payload[0].toInt()
        val fuHeader = payload[1].toInt()
        val start = fuHeader and 0x80 != 0
        val end = fuHeader and 0x40 != 0
        val nalType = fuHeader and 0x1F
        val reconstructedHeader = ((fuIndicator and 0xE0) or nalType).toByte()

        if (start) {
            fuBuffer = ByteArrayOutputStream().apply {
                write(START_CODE)
                write(reconstructedHeader.toInt())
            }
        }

        val buffer = fuBuffer ?: return
        buffer.write(payload, 2, payload.size - 2)

        if (end) {
            emitNalBuffer(buffer)
            fuBuffer = null
        }
    }

    private fun emitNal(nal: ByteArray) {
        val buffer = ByteArrayOutputStream()
        buffer.write(START_CODE)
        buffer.write(nal)
        emitNalBuffer(buffer)
    }

    private fun emitNalBuffer(buffer: ByteArrayOutputStream) {
        onNalUnit(buffer.toByteArray())
    }

    private fun parseSdp(baseUrl: String, sdp: String): RtspSessionInfo {
        var controlUrl = baseUrl
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        var width = DEFAULT_WIDTH
        var height = DEFAULT_HEIGHT
        var inVideo = false

        sdp.lineSequence().forEach { line ->
            when {
                line.startsWith("m=") -> {
                    inVideo = line.startsWith("m=video")
                }
                inVideo && line.startsWith("a=control:") -> {
                    val controlValue = line.substringAfter("a=control:")
                    controlUrl = when {
                        controlValue.startsWith("rtsp://") -> controlValue
                        controlValue == "*" -> baseUrl
                        baseUrl.endsWith("/") -> baseUrl + controlValue
                        else -> "$baseUrl/$controlValue"
                    }
                }
                inVideo && line.startsWith("a=fmtp:") -> {
                    val params = line.substringAfter(" ")
                    val match = Regex("sprop-parameter-sets=([^;\\s]+)").find(params)
                    val values = match?.groupValues?.getOrNull(1)?.split(',')
                    if (!values.isNullOrEmpty()) {
                        sps = values.getOrNull(0)?.let { Base64.decode(it, Base64.DEFAULT) }
                        pps = values.getOrNull(1)?.let { Base64.decode(it, Base64.DEFAULT) }
                    }
                }
                inVideo && line.startsWith("a=framesize:") -> {
                    val sizePart = line.substringAfter(' ').trim()
                    val parts = sizePart.split('-')
                    if (parts.size == 2) {
                        width = parts[0].toIntOrNull() ?: width
                        height = parts[1].toIntOrNull() ?: height
                    }
                }
            }
        }

        return RtspSessionInfo(controlUrl, sps, pps, width, height)
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: IOException) {
            // Ignore
        } finally {
            socket = null
        }
    }

    private fun readExactBytes(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read == -1) {
                throw IOException("Unexpected EOF while reading RTSP payload")
            }
            offset += read
        }
        return buffer
    }

    data class RtspResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: String
    )

    data class RtspSessionInfo(
        val controlUrl: String,
        val sps: ByteArray?,
        val pps: ByteArray?,
        val width: Int,
        val height: Int
    )

    private companion object {
        const val DEFAULT_RTSP_PORT = 554
        const val CONNECT_TIMEOUT_MS = 3000
        const val RTP_CHANNEL = 0
        const val INTERLEAVED_MAGIC = 0x24
        const val RTP_HEADER_SIZE = 12
        const val NAL_TYPE_STAP_A = 24
        const val NAL_TYPE_FU_A = 28
        val START_CODE = byteArrayOf(0, 0, 0, 1)
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
    }
}
