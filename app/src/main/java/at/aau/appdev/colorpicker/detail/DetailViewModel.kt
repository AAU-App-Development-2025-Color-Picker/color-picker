package at.aau.appdev.colorpicker.detail

import androidx.lifecycle.ViewModel
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


data class DetailUiState(
    val color: List<ColorEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DetailViewModel @Inject constructor(private val repository: Repository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = mutableUiState.asStateFlow()
}