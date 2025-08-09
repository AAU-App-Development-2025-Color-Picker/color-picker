package at.aau.appdev.colorpicker.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import at.aau.appdev.colorpicker.persistence.entity.PhotoEntity


@Dao
interface PhotoDao {
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long
}