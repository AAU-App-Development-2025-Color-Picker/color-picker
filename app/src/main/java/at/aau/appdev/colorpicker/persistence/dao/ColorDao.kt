package at.aau.appdev.colorpicker.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.ColorWithPhoto


@Dao
interface ColorDao {
    @Insert
    suspend fun insertColor(color: ColorEntity): Long

    @Query("SELECT * FROM colors")
    suspend fun getAll(): List<ColorEntity>

    @Query("SELECT * FROM colors WHERE id = :id")
    suspend fun getColorWithPhoto(id: Long): ColorWithPhoto
}