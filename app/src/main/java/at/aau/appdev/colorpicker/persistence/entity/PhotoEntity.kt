package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(tableName = "photos")
data class PhotoEntity @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val capturedAt: String = Clock.System.now().toString()
)
