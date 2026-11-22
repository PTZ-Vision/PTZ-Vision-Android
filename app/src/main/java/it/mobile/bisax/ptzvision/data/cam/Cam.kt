package it.mobile.bisax.ptzvision.data.cam

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** How PTZ commands (and, for NDI, the video) reach the camera. */
enum class CamProtocol {
    /** VISCA over IP (TCP) for control, RTSP for video. */
    VISCA,

    /** NDI®: control via NDI PTZ metadata, video via NDI (RTSP as fallback). */
    NDI,
}

@Entity(
    tableName = "cam",
    indices = [Index(value = ["ip", "controlPort", "streamPort", "httpPort", "ndiSourceName"], unique = true)],
)
data class Cam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ip: String,
    val controlPort: Int,
    val streamPort: Int,
    val httpPort: Int,
    val active: Boolean,
    val main: Boolean = false,
    val autofocus: Boolean = false,
    val aiTracking: Boolean = false,
    val pan: Float = 0f,
    val tilt: Float = 0f,
    val zoom: Float = 0f,
    val focus: Float = 0f,
    val iris: Float = 0f,
    @ColumnInfo(defaultValue = "VISCA")
    val protocol: CamProtocol = CamProtocol.VISCA,
    /** Full NDI source name (`MACHINE (SOURCE)`); empty for non-NDI cameras. */
    @ColumnInfo(defaultValue = "")
    val ndiSourceName: String = "",
) {
    val isNdi: Boolean get() = protocol == CamProtocol.NDI
}
