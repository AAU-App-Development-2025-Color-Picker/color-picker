package at.aau.appdev.colorpicker.camera

import android.content.Context
import android.os.Environment
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
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


data class Probe(
    val id: Long,
    val color: Color,
    val position: Coordinate,
)

data class CameraUiState(
    val probes: List<Probe> = emptyList(),
    val activeProbe: Probe? = null,
    val isLoading: Boolean = true,
)


@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: Repository, @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = mutableUiState.asStateFlow()


    private val pendingTaps = ConcurrentLinkedQueue<Coordinate>()

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

        this.mutableUiState.value = this.mutableUiState.value.copy(probes = emptyList())
    }

    fun putAllProbes(probes: List<Probe>) {
        this.mutableUiState.value = this.mutableUiState.value.copy(probes = probes)
    }

    fun captureProbe() {
        val activeProbe = uiState.value.activeProbe ?: return

        viewModelScope.launch {
            repository.insertColor(
                ColorEntity(
                    paletteId = null,
                    photoId = null,
                    red = activeProbe.color.red,
                    green = activeProbe.color.green,
                    blue = activeProbe.color.blue,
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun captureAllProbes() {
        val probes = uiState.value.probes
        if (probes.isEmpty()) return

        val directory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // TODO: The few lines below here should go into the coroutine scope.
        val file = File(directory, "${Clock.System.now()}")

        // TODO: Here we need to tell 'CameraRenderer' to grab the latest frame and put it into some
        //       buffer. The image in that buffer is usually in YUV format and we still need to
        //       convert it.

        viewModelScope.launch {
            val paletteId = repository.insertPalette(
                PaletteEntity(
                    name = null,
                )
            )
            val photoId = repository.insertPhoto(
                PhotoEntity(
                    uri = "",
                )
            )
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
}