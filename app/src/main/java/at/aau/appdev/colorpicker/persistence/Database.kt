package at.aau.appdev.colorpicker.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import at.aau.appdev.colorpicker.persistence.dao.ColorDao
import at.aau.appdev.colorpicker.persistence.dao.PaletteDao
import at.aau.appdev.colorpicker.persistence.dao.PhotoDao
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.entity.PhotoEntity


@Database(
    entities = [
        ColorEntity::class,
        PaletteEntity::class,
        PhotoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun paletteDao(): PaletteDao
    abstract fun photoDao(): PhotoDao
}