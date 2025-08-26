package at.aau.appdev.colorpicker.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.aau.appdev.colorpicker.persistence.entity.PhotoEntity


@Dao
interface PhotoDao {
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos")
    suspend fun getAll(): List<PhotoEntity>
}