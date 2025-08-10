package at.aau.appdev.colorpicker.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class GalleryUiState(
    val colors: List<ColorEntity> = emptyList(),
    val palettes: List<PaletteEntity> = emptyList(),
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

            val colors = repository.getColors()
            val palettes = repository.getPalettes()

            mutableUiState.update {
                it.copy(
                    colors = colors, palettes = palettes, isLoading = false
                )
            }
        }
    }
}