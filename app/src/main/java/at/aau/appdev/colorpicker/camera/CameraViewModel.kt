package at.aau.appdev.colorpicker.camera

import android.content.Context
import android.media.Image
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.appdev.colorpicker.IdGenerator
import at.aau.appdev.colorpicker.persistence.entity.ColorEntity
import at.aau.appdev.colorpicker.persistence.entity.PaletteEntity
import at.aau.appdev.colorpicker.persistence.entity.PhotoEntity
import at.aau.appdev.colorpicker.persistence.repository.Repository
import com.google.ar.core.Anchor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


data class Probe(
    val color: Color,
    val position: Coordinate,
    val isVisible: Boolean,
)

data class CameraUiState(
    val guidToProbe: Map<Long, Probe> = emptyMap(),
    val activeProbe: Probe? = null,
    val animationOffset: Offset = Offset.Unspecified,
    val isLoading: Boolean = true,
)

enum class CaptureStatus {
    NONE, SINGLE, ALL
}


@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: Repository, @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mutableUiState.asStateFlow()


    private val pendingTaps = ConcurrentLinkedQueue<Coordinate>()

    private var captureStatus = CaptureStatus.NONE

    fun produceTap(x: Float, y: Float) {
        pendingTaps.add(Coordinate(x, y))
    }

    fun getAllTaps(): List<Coordinate> {
        val taps = mutableListOf<Coordinate>()
        while (true) {
            taps.add(pendingTaps.poll() ?: break)
        }
        return taps
    }

    fun getCaptureStatus(): Boolean {
        return this.captureStatus != CaptureStatus.NONE
    }

    // TODO: Rename.
    fun onCaptureProbeClicked() {
        captureStatus = CaptureStatus.SINGLE
    }

    // TODO: Rename.
    fun onCaptureAllProbesClicked() {
        captureStatus = CaptureStatus.ALL
    }

    @OptIn(ExperimentalTime::class)
    fun onImageAvailable(image: Image) {
        val output = ByteArrayOutputStream()
        ColorFormatConverter.yuvToJpeg(image, output);
        image.close()

        val directory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(directory, "${Clock.System.now()}.jpeg")

        file.writeBytes(output.toByteArray())

        when (this.captureStatus) {
            CaptureStatus.NONE -> throw IllegalStateException()
            CaptureStatus.SINGLE -> captureProbe(file.path)
            CaptureStatus.ALL -> captureAllProbes(file.path)
        }

        this.captureStatus = CaptureStatus.NONE
    }

    // FIXME: Careful! It's possible that ARCore decides on its own to let go of anchors; this can
    //        be the case when an instant placement anchor cannot be fixed to virtual space in time.
    //        We need to aware of this and handle it gracefully.
    private val idGenerator = IdGenerator()
    private val guidToAnchor = mutableMapOf<Long, Anchor>()

    fun putAnchor(anchor: Anchor) {
        val id = idGenerator.next()
        guidToAnchor.put(id, anchor)
    }

    fun putAllAnchors(anchors: List<Anchor>) {
        for (anchor in anchors) {
            putAnchor(anchor)
        }
    }

    fun getAllAnchors(): List<Pair<Long, Anchor>> {
        return this.guidToAnchor.toList()
    }

    private fun clearAllAnchors() {
        this.guidToAnchor.values.map(Anchor::detach)
        this.guidToAnchor.clear()

        clearAllProbes()
    }

    fun putAllProbes(guidToProbe: Map<Long, Probe>) {
        this.mutableUiState.value = this.mutableUiState.value.copy(
            guidToProbe = this.mutableUiState.value.guidToProbe + guidToProbe
        )
    }

    private fun clearAllProbes() {
        this.mutableUiState.value = this.mutableUiState.value.copy(
            guidToProbe = this.mutableUiState.value.guidToProbe.map { (key, value) ->
                key to value.copy(isVisible = false)
            }.toMap()
        )
    }

    fun captureProbe(imageUri: String) {
        val activeProbe = uiState.value.activeProbe ?: return

        viewModelScope.launch {
            val paletteId = null
            val photoId = repository.insertPhoto(PhotoEntity(uri = imageUri))
            repository.insertColor(
                ColorEntity(
                    paletteId = paletteId,
                    photoId = photoId,
                    red = activeProbe.color.red,
                    green = activeProbe.color.green,
                    blue = activeProbe.color.blue,
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun captureAllProbes(imageUri: String) {
        val probes = uiState.value.guidToProbe.values
        if (probes.isEmpty()) return

        viewModelScope.launch {
            val paletteId = repository.insertPalette(PaletteEntity(name = null))
            val photoId = repository.insertPhoto(PhotoEntity(uri = imageUri))
            probes.forEach { probe ->
                repository.insertColor(
                    ColorEntity(
                        paletteId = paletteId,
                        photoId = photoId,
                        red = probe.color.red,
                        green = probe.color.green,
                        blue = probe.color.blue,
                    )
                )
            }
        }

        clearAllAnchors()
    }

    fun setAnimationOffset(offset: Offset) {
        this.mutableUiState.value = this.mutableUiState.value.copy(animationOffset = offset)
    }
}