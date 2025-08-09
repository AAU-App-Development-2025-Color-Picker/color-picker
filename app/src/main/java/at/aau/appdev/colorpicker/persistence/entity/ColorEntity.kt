package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
data class ColorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paletteId: Long?,
    val photoId: Long?,
    val hexCode: String,
    val labL: Double?,
    val labA: Double?,
    val labB: Double?,
    val capturedAt: Long = System.currentTimeMillis()
)
