package at.aau.appdev.colorpicker.persistence.repository

import at.aau.appdev.colorpicker.persistence.dao.ColorDao
import at.aau.appdev.colorpicker.persistence.dao.PaletteDao
import at.aau.appdev.colorpicker.persistence.dao.PhotoDao
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteWithColors
import at.aau.appdev.colorpicker.persistence.entity.PhotoEntity
import javax.inject.Inject

class Repository @Inject constructor(
    private val colorDao: ColorDao,
    private val paletteDao: PaletteDao,
    private val photoDao: PhotoDao,
) {

    suspend fun insertColor(color: ColorEntity): Long {
        return colorDao.insertColor(color);
    }

    suspend fun getColors(): List<ColorEntity> {
        return colorDao.getAll();
    }

    suspend fun insertPalette(palette: PaletteEntity): Long {
        return paletteDao.insertPalette(palette);
    }

    suspend fun getPalettes(): List<PaletteEntity> {
        return paletteDao.getAll();
    }

    suspend fun getPalettesWithColors(): List<PaletteWithColors> {
        return paletteDao.getAllWithColors();
    }

    suspend fun insertPhoto(photo: PhotoEntity): Long {
        return photoDao.insertPhoto(photo);
    }

    suspend fun getPhotos(): List<PhotoEntity> {
        return photoDao.getAll();
    }
}