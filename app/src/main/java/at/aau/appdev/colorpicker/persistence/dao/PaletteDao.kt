package at.aau.appdev.colorpicker.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteWithColor


@Dao
interface PaletteDao {
    @Insert
    suspend fun insertPalette(palette: PaletteEntity): Long

    @Query("SELECT * FROM palettes")
    suspend fun getAllPalettes(): List<PaletteEntity>

    @Query("SELECT * FROM palettes WHERE id = :id")
    suspend fun getPaletteWithColors(id: Long): PaletteWithColor
}