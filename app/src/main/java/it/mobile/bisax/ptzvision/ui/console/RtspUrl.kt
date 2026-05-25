package it.mobile.bisax.ptzvision.ui.console

import it.mobile.bisax.ptzvision.data.cam.Cam

internal fun Cam.rtspUrl(): String = "rtsp://$ip:$streamPort/2"
