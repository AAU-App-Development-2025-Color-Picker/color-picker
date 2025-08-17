package at.aau.appdev.colorpicker.camera

import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotTrackingException

object ARCoreInteractionHandler {

    // FIXME: Rename this function at some point.
    fun somethingThatHappensEachFrame(
        consumeTaps: () -> List<Pair<Float, Float>>, consumeAnchors: () -> List<Anchor>, produceAnchors: (List<Anchor>) -> Unit
    ): (Frame) -> Unit {
        return { frame ->
            // ~~ First act: Project coordinates and sample colors. ~~
            // TODO: We can already project coordinates using 'projectAnchorToScreen(...)'.
            //       This should be applied to each and every anchor in the view model, after which
            //       a list of coordinates should be returned. We won't directly return the list,
            //       however, as we will also add the corresponding color to each coordinate!
            // INFO: To access anchors, we probably have to add another argument to the signature of
            //       this function - 'consumeAnchors: () -> List<Anchor>' or something.
            // TODO: Next step: We need to sample colors from the

            // ~~ Second act: Interaction handling. ~~
            consumeTapsAndProduceAnchors(frame, consumeTaps, produceAnchors)
            Log.d("", "")
        }
    }

    fun consumeTapsAndProduceAnchors(
        frame: Frame,
        consumeTaps: () -> List<Pair<Float, Float>>,
        produceAnchors: (List<Anchor>) -> Unit,
    ): Unit {
        val taps = consumeTaps()
        if (taps.isEmpty()) return

        val anchors = mutableListOf<Anchor>()

        for ((x, y) in taps) {
            val hits = frame.hitTestInstantPlacement(x, y, 3.0f)
            if (hits.isNotEmpty()) {
                val hit = hits.first()
                if (hit.trackable.trackingState == TrackingState.TRACKING) {
                    try {
                        anchors += hit.createAnchor()
                    } catch (_: NotTrackingException) {
                        // NO-OP.
                    }
                }
            }
        }

        if (anchors.isEmpty()) return
        produceAnchors(anchors)
    }

    // https://learnopengl.com/getting-started/transformations
    // https://learnopengl.com/getting-started/coordinate-systems
    fun projectAnchorToScreen(/* display: Display, */ frame: Frame, anchor: Anchor
    ): Pair<Float, Float>? {
        val projMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val worldMatrix = FloatArray(16)

        frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f)
        frame.camera.getViewMatrix(viewMatrix, 0)
        anchor.pose.toMatrix(worldMatrix, 0)

        val worldOrigin = floatArrayOf(0f, 0f, 0f, 1f)

        val cameraMatrix = FloatArray(16)
        Matrix.multiplyMM(cameraMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val worldCoords = FloatArray(4)
        Matrix.multiplyMV(worldCoords, 0, worldMatrix, 0, worldOrigin, 0)

        val clipCoords = FloatArray(4)
        Matrix.multiplyMV(clipCoords, 0, cameraMatrix, 0, worldCoords, 0)

        if (clipCoords[3] <= 0f) {
            return null
        }
        val normalizedX = clipCoords[0] / clipCoords[3]
        val normalizedY = clipCoords[1] / clipCoords[3]

        // Convert normalized device coordinates (-1..1, 1..-1) to screen space coordinates (0..w, 0..h):
        // FIXME: The width and height should be taken from the actual display used!
        val screenX = (normalizedX * 0.5f + 0.5f) * 1080 // display.width
        val screenY = (1f - (normalizedY * 0.5f + 0.5f)) * 2400 // display.height

        return Pair(screenX, screenY)
    }
}