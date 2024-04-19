package it.mobile.bisax.ptzvision.data.cam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.mobile.bisax.ptzvision.data.preset.Preset

@Entity(
    tableName = "cam",
    indices = [Index(value = ["ip"], unique = true)],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Preset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class Cam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ip: String,
    val port: Int,
    val presetId: Int,
    val active: Boolean,
    val pan: Float,
    val tilt: Float,
    val zoom: Float,
    val focus: Float,
    val iris: Float,
    val autofocus: Boolean,
    val aiTracking: Boolean
)
