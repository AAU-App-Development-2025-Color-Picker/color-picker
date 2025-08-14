package at.aau.appdev.colorpicker.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.gallery.GalleryUiState
import at.aau.appdev.colorpicker.persistence.repository.Repository
import com.google.ar.core.Anchor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject


data class CameraUiState(
    val isLoading: Boolean = true,
)

class CameraViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    private val mutableUiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = mutableUiState.asStateFlow()

    private val mutableAnchors = MutableStateFlow<List<Anchor>>(emptyList())
    val anchors: StateFlow<List<Anchor>> = mutableAnchors.asStateFlow()

    private val pendingTaps = ConcurrentLinkedQueue<Pair<Float, Float>>()

    fun registerTap(x: Float, y: Float) {
        pendingTaps.add(Pair(x, y))
    }

    fun consumeTaps(): List<Pair<Float, Float>> {
        val taps = mutableListOf<Pair<Float, Float>>()
        while (true) {
            taps.add(pendingTaps.poll() ?: break)
        }
        return taps
    }

    fun addAnchors(newAnchors: List<Anchor>) {
        if (newAnchors.isEmpty()) return

        viewModelScope.launch {
            mutableAnchors.value = mutableAnchors.value + newAnchors
        }
    }
}