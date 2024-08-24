package it.mobile.bisax.ptzvision.data.cam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cam",
    indices = [Index(value = ["ip", "controlPort", "streamPort", "httpPort"], unique = true)],
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
)