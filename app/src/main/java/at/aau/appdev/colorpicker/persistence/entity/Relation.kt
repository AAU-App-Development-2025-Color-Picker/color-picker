package at.aau.appdev.colorpicker.persistence.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PaletteWithColor(
    @Embedded val palette: PaletteEntity, @Relation(
        parentColumn = "id",
        entityColumn = "paletteId",
    ) val colors: List<ColorEntity>
)

data class ColorWithPhoto(
    @Embedded val color: ColorEntity, @Relation(
        parentColumn = "photoId",
        entityColumn = "id",
    ) val photo: PhotoEntity?
)
