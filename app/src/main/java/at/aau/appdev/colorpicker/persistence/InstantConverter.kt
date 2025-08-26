package at.aau.appdev.colorpicker.persistence


import androidx.room.TypeConverter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// https://developer.android.com/training/data-storage/room/referencing-data
class InstantConverter {
    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun fromString(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun toString(instant: Instant?): String? {
        return instant?.toString()
    }
}