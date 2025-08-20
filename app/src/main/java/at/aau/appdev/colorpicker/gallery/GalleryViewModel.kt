package at.aau.appdev.colorpicker.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteWithColors
import at.aau.appdev.colorpicker.persistence.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class GalleryItem {
    data class Swatch(val color: ColorEntity) : GalleryItem()
    data class Palette(val palette: PaletteWithColors) : GalleryItem()
}

data class GalleryUiState(
    val items: List<GalleryItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class GalleryViewModel @Inject constructor(private val repository: Repository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = mutableUiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true) }

            val swatches = repository.getColorsWithoutPalette()
            val palettesWithColors = repository.getPalettesWithColors()

            val galleryItems = mutableListOf<GalleryItem>()

            swatches.forEach { color ->
                galleryItems.add(GalleryItem.Swatch(color))
            }
            palettesWithColors.forEach { paletteWithColor ->
                galleryItems.add(GalleryItem.Palette(paletteWithColor))
            }

            mutableUiState.update {
                it.copy(
                    items = galleryItems,
                    isLoading = false,
                )
            }
        }
    }
}