package at.aau.appdev.colorpicker.camera

import android.util.Log
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


data class CameraUiState(
    val points: List<Pair<Float, Float>> = emptyList(),
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

    // FIXME: This function here maybe needs to do something else?
    //        Our view model needs to track anchors. It it sadly not possible to let ARCore handle
    //        the anchors on its own because we need to be able to a) reset all anchors at once and
    //        b) reset single anchors if a user just wants to move them around.
    //        Use case a) can be addressed without separately tracking the points, use case b) not.
    //
    // TODO: We track new anchors whenever they are created – just like we already do right now.
    //       Additionally, we add a list of colors/coordinates to the UI state; this list has to be
    //       updated >>EVERY<< frame! Why? Because the world understanding of ARCore changes every
    //       frame! So we have to trigger a calculation on each frame which translates the anchors
    //       into (screen-space) coordinates for the UI state.
    fun produceAnchors(newAnchors: List<Anchor>) {
        viewModelScope.launch {
            anchors.addAll(newAnchors)
            // TODO: Update the 'CameraUiState'!
            //       -> 'projectAnchorToScreen(...)'.
        }
    }
}