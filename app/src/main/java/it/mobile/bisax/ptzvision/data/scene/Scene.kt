package it.mobile.bisax.ptzvision.data.scene

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.mobile.bisax.ptzvision.data.cam.Cam

@Entity(
    tableName = "scene",
    indices = [Index(value = ["idCamera"])],
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
    val idCamera: Int,
    val pan: Float,
    val tilt: Float,
    val zoom: Float,
    val focus: Float,
    val iris: Float,
    val panSpeed: Float,
    val tiltSpeed: Float,
    val zoomSpeed: Float,
    val focusSpeed: Float
)