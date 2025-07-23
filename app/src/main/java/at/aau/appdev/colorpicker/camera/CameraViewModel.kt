package at.aau.appdev.colorpicker.camera

import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import kotlinx.coroutines.launch

data class ColorProbeData(
    val id: Int,
    val color: Color,
    val position: IntOffset,
    val anchor: Anchor? = null,
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class CameraState(
    val isSessionActive: Boolean = true,
    val isPhotoMode: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isCapturing: Boolean = false
)

class CameraViewModel : ViewModel() {

    // StateFlow for UI state observation
    private val _colorProbes = MutableStateFlow<List<ColorProbeData>>(emptyList())
    val colorProbes: StateFlow<List<ColorProbeData>> = _colorProbes.asStateFlow()

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _activeProbeId = MutableStateFlow<Int?>(null)
    val activeProbeId: StateFlow<Int?> = _activeProbeId.asStateFlow()

    // Internal state management
    private var nextProbeId = 0
    private val anchors = mutableMapOf<Int, Anchor>()

    init {
        Log.d("CameraViewModel", "ViewModel initialized")
    }

    /**
     * Handles tap gestures on the camera view to create new color probes
     */
    fun onCameraTap(offset: Offset, hitResults: List<HitResult>, renderer: CameraRenderer) {
        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Processing camera tap at ${offset.x}, ${offset.y}")

                if (hitResults.isNotEmpty()) {
                    // Every anchor is stored in the 'viewModel' so it can be properly disposed of when it is not needed anymore:
                    val hitResult = hitResults.first()
                    val anchor = hitResult.createAnchor()

                    // Sample color from the camera frame at the tap position
                    val sampledColor = sampleColorFromFrame(offset, renderer)

                    val newProbe = ColorProbeData(
                        id = nextProbeId++,
                        color = sampledColor,
                        position = IntOffset(offset.x.toInt(), offset.y.toInt()),
                        anchor = anchor,
                        isActive = false
                    )

                    anchors[newProbe.id] = anchor
                    addColorProbe(newProbe)

                    Log.d("CameraViewModel", "Created new color probe with ID: ${newProbe.id}")
                } else {
                    Log.w("CameraViewModel", "No hit results available for tap")
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing camera tap", e)
            }
        }
    }

    /**
     * Activates a color probe for interaction
     */
    fun onProbeSelected(probeId: Int) {
        _activeProbeId.value = probeId
        updateProbeActiveState(probeId, true)
        Log.d("CameraViewModel", "Activated probe: $probeId")
    }

    /**
     * Deactivates the currently active probe
     */
    fun onProbeDeselected() {
        _activeProbeId.value?.let { activeId ->
            updateProbeActiveState(activeId, false)
        }
        _activeProbeId.value = null
        Log.d("CameraViewModel", "Deactivated active probe")
    }

    /**
     * Handles single tap on a color probe
     */
    fun onProbeSingleTap(probeId: Int) {
        Log.d("CameraViewModel", "Single tap on probe: $probeId")
        onProbeSelected(probeId)
    }

    /**
     * Handles double tap on a color probe - typically for deletion
     */
    fun onProbeDoubleTap(probeId: Int) {
        Log.d("CameraViewModel", "Double tap on probe: $probeId - removing probe")
        removeColorProbe(probeId)
    }

    /**
     * Handles drag start for a color probe
     */
    fun onProbeDragStart(probeId: Int, startOffset: Offset) {
        onProbeSelected(probeId)
        Log.d("CameraViewModel", "Started dragging probe: $probeId")
    }

    /**
     * Handles drag end for a color probe
     */
    fun onProbeDragEnd(probeId: Int) {
        onProbeDeselected()
        Log.d("CameraViewModel", "Finished dragging probe: $probeId")
    }

    /**
     * Updates the position of a color probe during drag
     */
    fun updateProbePosition(probeId: Int, newPosition: IntOffset) {
        val currentProbes = _colorProbes.value ?: return
        val updatedProbes = currentProbes.map { probe ->
            if (probe.id == probeId) {
                probe.copy(position = newPosition)
            } else {
                probe
            }
        }
        _colorProbes.value = updatedProbes
    }

    /**
     * Captures the current color probe as a single entry
     */
    fun captureCurrentProbe() {
        viewModelScope.launch {
            val activeId = _activeProbeId.value
            if (activeId != null) {
                val probe = _colorProbes.value?.find { it.id == activeId }
                if (probe != null) {
                    saveProbeToGallery(listOf(probe))
                    Log.d("CameraViewModel", "Captured single probe: ${probe.id}")
                }
            } else {
                Log.w("CameraViewModel", "No active probe to capture")
            }
        }
    }

    /**
     * Captures all current color probes
     */
    fun captureAllProbes() {
        viewModelScope.launch {
            val probes = _colorProbes.value ?: emptyList()
            if (probes.isNotEmpty()) {
                saveProbeToGallery(probes)
                Log.d("CameraViewModel", "Captured ${probes.size} probes")
            } else {
                Log.w("CameraViewModel", "No probes to capture")
            }
        }
    }

    /**
     * Handles photo library selection
     */
    fun onPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            Log.d("CameraViewModel", "Photo selected: $uri")

            // Update camera state to photo mode
            _cameraState.value = _cameraState.value.copy(
                isSessionActive = false,
                isPhotoMode = true,
                selectedImageUri = uri
            )

            // Clear existing probes when switching to photo mode
            clearAllProbes()
        }
    }

    /**
     * Switches back to camera mode from photo mode
     */
    fun switchToCameraMode() {
        viewModelScope.launch {
            Log.d("CameraViewModel", "Switching to camera mode")

            _cameraState.value = _cameraState.value.copy(
                isSessionActive = true,
                isPhotoMode = false,
                selectedImageUri = null
            )

            clearAllProbes()
        }
    }

    /**
     * Clears all color probes and disposes of anchors
     */
    fun clearAllProbes() {
        // Dispose of all anchors
        anchors.values.forEach { anchor ->
            try {
                anchor.detach()
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error detaching anchor", e)
            }
        }
        anchors.clear()

        _colorProbes.value = emptyList()
        _activeProbeId.value = null

        Log.d("CameraViewModel", "Cleared all probes and anchors")
    }

    // Private helper methods

    private fun addColorProbe(probe: ColorProbeData) {
        val currentProbes = _colorProbes.value ?: emptyList()
        _colorProbes.value = currentProbes + probe
    }

    private fun removeColorProbe(probeId: Int) {
        // Dispose of the anchor
        anchors[probeId]?.let { anchor ->
            try {
                anchor.detach()
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error detaching anchor for probe $probeId", e)
            }
        }
        anchors.remove(probeId)

        // Remove from probe list
        val currentProbes = _colorProbes.value ?: return
        _colorProbes.value = currentProbes.filter { it.id != probeId }

        // Deactivate if this was the active probe
        if (_activeProbeId.value == probeId) {
            _activeProbeId.value = null
        }
    }

    private fun updateProbeActiveState(probeId: Int, isActive: Boolean) {
        val currentProbes = _colorProbes.value ?: return
        val updatedProbes = currentProbes.map { probe ->
            if (probe.id == probeId) {
                probe.copy(isActive = isActive)
            } else {
                probe.copy(isActive = false) // Ensure only one probe is active
            }
        }
        _colorProbes.value = updatedProbes
    }

    /**
     * Samples color from the camera frame at the given position
     * This is a placeholder implementation - actual implementation would
     * read pixel data from the OpenGL frame buffer
     */
    private fun sampleColorFromFrame(offset: Offset, renderer: CameraRenderer): Color {
        // TODO: Implement actual color sampling from the camera frame
        // INFO: This would involve reading pixel data from the OpenGL frame buffer
        // INFO: at the specified coordinates and converting to a Color object

        // INFO: For now, return a placeholder color based on position
        val hue = (offset.x + offset.y) % 360f
        return Color.hsv(hue, 0.8f, 0.9f)
    }

    /**
     * Saves color probes to the gallery/database
     * This would typically involve database operations or API calls
     */
    private suspend fun saveProbeToGallery(probes: List<ColorProbeData>) {
        try {
            // TODO: Implement actual saving logic
            // - Save to local database
            // - Create gallery entries

            Log.d("CameraViewModel", "Saving ${probes.size} probes to gallery")

            // Placeholder implementation
            probes.forEach { probe ->
                Log.d("CameraViewModel", "Saved probe ${probe.id} with color ${probe.color}")
            }

        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error saving probes to gallery", e)
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Clean up all anchors when ViewModel is destroyed
        clearAllProbes()

        Log.d("CameraViewModel", "ViewModel cleared")
    }
}