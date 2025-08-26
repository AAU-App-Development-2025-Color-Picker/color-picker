package at.aau.appdev.colorpicker.camera

import android.media.Image
import com.google.ar.core.Frame

object ARCoreCaptureHandler {

    fun consumeStatusAndProduceImage(
        consumeStatus: () -> Boolean,
        produceImage: (Image) -> Unit,
    ): (Frame) -> Unit {
        return { frame ->
            consumeStatusAndProduceImage(frame, consumeStatus, produceImage)
        }
    }

    fun consumeStatusAndProduceImage(
        frame: Frame,
        consumeStatus: () -> Boolean,
        produceImage: (Image) -> Unit,
    ) {
        if (!consumeStatus()) return

        try {
            val image = frame.acquireCameraImage()
            produceImage(image)
        } catch (e: Exception) {

        }
    }
}