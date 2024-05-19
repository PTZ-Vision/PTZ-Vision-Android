package it.mobile.bisax.ptzvision.data.cam

import android.content.Context
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.controller.ViscaPTZController
import it.mobile.bisax.ptzvision.controller.utils.MathUtils
import it.mobile.bisax.ptzvision.data.utils.Protocol

@Entity(
    tableName = "cam",
    indices = [Index(value = ["ip"], unique = true)],
)
data class Cam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ip: String,
    val port: Int,
    val active: Boolean,
    val main: Boolean = false,
    val autofocus: Boolean = false,
    val aiTracking: Boolean = false,
    val presetId: Int=0,
    val pan: Float = 0f,
    val tilt: Float = 0f,
    val zoom: Float = 0f,
    val focus: Float = 0f,
    val iris: Float = 0f,
)