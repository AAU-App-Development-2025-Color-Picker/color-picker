package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "colors", foreignKeys = [ForeignKey(
        entity = PaletteEntity::class,
        parentColumns = ["id"],
        childColumns = ["paletteId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = PhotoEntity::class,
        parentColumns = ["id"],
        childColumns = ["photoId"],
        onDelete = ForeignKey.SET_NULL
    )], indices = [Index("paletteId"), Index("photoId")]
)
data class ColorEntity @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paletteId: Long?,
    val photoId: Long?,
    val red: Float,
    val green: Float,
    val blue: Float,
    val capturedAt: String = Clock.System.now().toString()
)
