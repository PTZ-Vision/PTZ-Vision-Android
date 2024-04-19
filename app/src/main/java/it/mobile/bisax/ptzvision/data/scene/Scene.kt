package it.mobile.bisax.ptzvision.data.scene

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import it.mobile.bisax.ptzvision.data.cam.Cam

@Entity(
    tableName = "scene",
    foreignKeys = [
        ForeignKey(
            entity = Cam::class,
            parentColumns = ["id"],
            childColumns = ["idCamera"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Scene(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val slot: Int,
    val name: String,
    val pan: Float,
    val tilt: Float,
    val zoom: Float,
    val focus: Float,
    val iris: Float,
    val idCamera: Int,
    val panSpeed: Float,
    val tiltSpeed: Float,
    val zoomSpeed: Float,
    val focusSpeed: Float
)