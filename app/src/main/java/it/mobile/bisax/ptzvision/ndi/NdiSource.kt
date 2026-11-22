package it.mobile.bisax.ptzvision.ndi

/**
 * A source found on the network.
 *
 * @param fullName the canonical NDI name, `MACHINE (SOURCE)`: this is what identifies the source.
 * @param address the network address (`ip:port`) when known; not stable across reboots/DHCP.
 */
data class NdiSource(
    val fullName: String,
    val address: String? = null,
) {
    /** The part between parentheses, e.g. `PTZ-1` in `BIRDDOG-1234 (PTZ-1)`. */
    val sourceName: String
        get() = parseNdiName(fullName).second

    /** The machine/device part, e.g. `BIRDDOG-1234`. */
    val machineName: String
        get() = parseNdiName(fullName).first

    /** Host part of [address] (no port), when known. */
    val host: String?
        get() = address?.let(::hostOf)
}

/** Splits `MACHINE (SOURCE)` into its two parts. Names without parentheses are returned as-is. */
fun parseNdiName(fullName: String): Pair<String, String> {
    val open = fullName.indexOf(" (")
    if (open < 0 || !fullName.endsWith(")")) return fullName to fullName
    return fullName.substring(0, open) to fullName.substring(open + 2, fullName.length - 1)
}

/** Extracts the host from an NDI url address such as `192.168.1.10:5961`. */
fun hostOf(address: String): String? {
    val trimmed = address.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("[")) return trimmed.substringAfter('[').substringBefore(']').ifEmpty { null }
    return if (trimmed.count { it == ':' } == 1) trimmed.substringBefore(':') else trimmed
}

/** Decodes the `[name, url, name, url, ...]` array returned by the native finder. */
internal fun decodeSources(flat: Array<String>): List<NdiSource> =
    flat.toList().chunked(2)
        .filter { it.size == 2 && it[0].isNotBlank() }
        .map { NdiSource(it[0], it[1].ifBlank { null }) }
        .sortedBy { it.fullName.lowercase() }
