package at.aau.appdev.colorpicker.camera

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.persistence.repository.Repository
import com.google.ar.core.Anchor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject


data class Point(
    val color: Color,
    val position: Pair<Float, Float>
)

data class CameraUiState(
    val points: List<Point> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CameraViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    private val mutableUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mutableUiState.asStateFlow()

    private val anchors = mutableListOf<Anchor>()
    private val pendingTaps = ConcurrentLinkedQueue<Pair<Float, Float>>()

    fun registerTap(x: Float, y: Float) {
        Log.d("CameraViewModel.registerTap", "${pendingTaps.size} pending taps before.")
        pendingTaps.add(Pair(x, y))
        Log.d("CameraViewModel.registerTap", "${pendingTaps.size} pending taps after.")
    }

    fun consumeTaps(): List<Pair<Float, Float>> {
        val taps = mutableListOf<Pair<Float, Float>>()
        while (true) {
            taps.add(pendingTaps.poll() ?: break)
        }
        return taps
    }

    fun produceAnchors(newAnchors: List<Anchor>) {
        viewModelScope.launch {
            anchors.addAll(newAnchors)
        }
    }

    fun consumeAnchors(): List<Anchor> {
        return this.anchors
    }

    fun producePoints(points: List<Point>) {
        this.mutableUiState.value = this.mutableUiState.value.copy(points = points)
    }
}