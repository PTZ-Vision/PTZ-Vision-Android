package it.mobile.bisax.ptzvision.data.preset

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.mobile.bisax.ptzvision.data.utils.Protocol

@Entity(
    tableName = "preset",
)
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val protocol: Protocol,
    val maxPan: String,
    val maxPanSpeed: Float,
    val maxTilt: String,
    val maxTiltSpeed: Float,
    val maxZoom: String,
    val maxZoomSpeed: Float,
    val maxFocus: String,
    val maxFocusSpeed: Float,
    val maxIris: String,
)
