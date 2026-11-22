package it.mobile.bisax.ptzvision.ui.console.zerocopy

import java.io.ByteArrayOutputStream

/** Helpers to build RTP/H.264 packets for tests. */
internal object Rtp {
    val START_CODE = byteArrayOf(0, 0, 0, 1)

    // NAL headers (forbidden=0, nri=3)
    const val NAL_SLICE = 0x61   // type 1, nri 3
    const val NAL_IDR = 0x65     // type 5
    const val NAL_SPS = 0x67     // type 7
    const val NAL_PPS = 0x68     // type 8
    const val NAL_AUD = 0x09     // type 9

    fun nal(header: Int, vararg payload: Int): ByteArray =
        byteArrayOf(header.toByte()) + payload.map { it.toByte() }.toByteArray()

    fun packet(
        seq: Int,
        timestamp: Long,
        payload: ByteArray,
        marker: Boolean = true,
        payloadType: Int = 96,
        csrcCount: Int = 0,
        extensionWords: Int = -1,
        padding: Int = 0,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        var b0 = 0x80 or (csrcCount and 0x0F)
        if (extensionWords >= 0) b0 = b0 or 0x10
        if (padding > 0) b0 = b0 or 0x20
        out.write(b0)
        out.write((if (marker) 0x80 else 0) or (payloadType and 0x7F))
        out.write(seq shr 8 and 0xFF); out.write(seq and 0xFF)
        for (shift in intArrayOf(24, 16, 8, 0)) out.write((timestamp shr shift).toInt() and 0xFF)
        repeat(4) { out.write(0x11) } // SSRC
        repeat(csrcCount * 4) { out.write(0x22) }
        if (extensionWords >= 0) {
            out.write(0xBE); out.write(0xDE)
            out.write(extensionWords shr 8); out.write(extensionWords and 0xFF)
            repeat(extensionWords * 4) { out.write(0x33) }
        }
        out.write(payload)
        if (padding > 0) {
            repeat(padding - 1) { out.write(0) }
            out.write(padding)
        }
        return out.toByteArray()
    }

    /** STAP-A aggregation of [nals]. */
    fun stapA(vararg nals: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0x78) // nri 3, type 24
        for (n in nals) {
            out.write(n.size shr 8); out.write(n.size and 0xFF); out.write(n)
        }
        return out.toByteArray()
    }

    /** Splits [nal] into FU-A fragments carrying at most [chunk] payload bytes each. */
    fun fuA(nal: ByteArray, chunk: Int): List<ByteArray> {
        val header = nal[0].toInt() and 0xFF
        val indicator = (header and 0xE0) or 28
        val type = header and 0x1F
        val body = nal.copyOfRange(1, nal.size)
        val parts = body.toList().chunked(chunk).map { it.toByteArray() }
        return parts.mapIndexed { i, part ->
            var fu = type
            if (i == 0) fu = fu or 0x80
            if (i == parts.lastIndex) fu = fu or 0x40
            byteArrayOf(indicator.toByte(), fu.toByte()) + part
        }
    }

    fun annexB(vararg nals: ByteArray): ByteArray =
        nals.fold(ByteArray(0)) { acc, n -> acc + START_CODE + n }
}

/** Records everything an [RtspNalExtractor.Listener] receives (copying reused buffers). */
internal class RecordingListener : RtspNalExtractor.Listener {
    data class AccessUnit(val data: ByteArray, val key: Boolean)

    val accessUnits = mutableListOf<AccessUnit>()
    val parameterSets = mutableListOf<Pair<ByteArray, ByteArray>>()
    var connected = false
    var unsupported: String? = null
    var error: String? = null

    override fun onParameterSets(sps: ByteArray, pps: ByteArray) {
        parameterSets += sps to pps
    }

    override fun onAccessUnit(data: ByteArray, size: Int, isKeyFrame: Boolean) {
        accessUnits += AccessUnit(data.copyOf(size), isKeyFrame)
    }

    override fun onConnected() {
        connected = true
    }

    override fun onUnsupported(message: String) {
        unsupported = message
    }

    override fun onError(message: String) {
        error = message
    }
}
