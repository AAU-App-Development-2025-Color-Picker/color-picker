package at.aau.appdev.colorpicker.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt


data class DetailUiState(
    val color: ColorEntity? = null,
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f,
    val hex: String = "#000000",
    val isLoading: Boolean = true,
)

@HiltViewModel
class DetailViewModel @Inject constructor(private val repository: Repository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = mutableUiState.asStateFlow()

    fun loadColorById(id: Long){

        viewModelScope.launch {

            val entity = repository.getColorById(id)
            if (entity != null) {

                val r = entity.red.coerceIn(0f, 1f)
                val g = entity.green.coerceIn(0f, 1f)
                val b = entity.blue.coerceIn(0f, 1f)
                mutableUiState.value = DetailUiState(

                    color = entity,
                    red = r,
                    green = g,
                    blue = b,
                    hex = rgbToHex(r, g, b),
                    isLoading = false

                )

            } else{

                mutableUiState.update { it.copy(isLoading = false) }

            }

        }

    }

    private fun rgbToHex(r: Float, g: Float, b: Float): String {
        val rr = (r.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        val gg = (g.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        val bb = (b.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", rr, gg, bb)
    }

    fun updateRed(value: Float) {
        mutableUiState.update { state ->
            val r = value.coerceIn(0f, 1f)
            state.copy(
                red = r,
                hex = rgbToHex(r, state.green, state.blue)
            )
        }
    }

    fun updateGreen(value: Float) {
        mutableUiState.update { state ->
            val g = value.coerceIn(0f, 1f)
            state.copy(
                green = g,
                hex = rgbToHex(state.red, g, state.blue)
            )
        }
    }

    fun updateBlue(value: Float) {
        mutableUiState.update { state ->
            val b = value.coerceIn(0f, 1f)
            state.copy(
                blue = b,
                hex = rgbToHex(state.red, state.green, b)
            )
        }
    }
}