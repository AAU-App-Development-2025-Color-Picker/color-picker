package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(tableName = "palettes")
data class PaletteEntity @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val capturedAt: String = Clock.System.now().toString()
)