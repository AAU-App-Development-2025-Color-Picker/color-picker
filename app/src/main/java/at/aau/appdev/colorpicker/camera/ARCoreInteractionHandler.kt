package at.aau.appdev.colorpicker.camera

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotTrackingException

object ARCoreInteractionHandler {

    fun consumeTapsAndProduceAnchors(
        consumeTaps: () -> List<Pair<Float, Float>>,
        produceAnchors: (List<Anchor>) -> Unit,
    ): (Frame) -> Unit {
        return { frame ->
            consumeTapsAndProduceAnchors(frame, consumeTaps, produceAnchors)
            Log.d("", "")
        }
    }

    fun consumeTapsAndProduceAnchors(
        frame: Frame,
        consumeTaps: () -> List<Pair<Float, Float>>,
        produceAnchors: (List<Anchor>) -> Unit,
    ) {
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
}