package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val capturedAt: Long = System.currentTimeMillis()
)
