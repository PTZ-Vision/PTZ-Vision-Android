package it.mobile.bisax.ptzvision.data.utils

enum class Protocol(
    val value: String
) {
    NDI("NDI"),
    VISCA("VISCA"),
    ONVIF("ONVIF"),
    CGI_HTTP("CGI-HTTP"),
}