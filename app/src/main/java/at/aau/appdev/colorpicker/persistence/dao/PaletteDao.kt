package at.aau.appdev.colorpicker.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteWithColors


@Dao
interface PaletteDao {
    @Insert
    suspend fun insertPalette(palette: PaletteEntity): Long

    @Query("SELECT * FROM palettes")
    suspend fun getAll(): List<PaletteEntity>

    @Query("SELECT * FROM palettes")
    suspend fun getAllWithColors(): List<PaletteWithColors>
}