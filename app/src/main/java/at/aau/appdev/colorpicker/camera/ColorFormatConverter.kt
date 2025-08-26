package at.aau.appdev.colorpicker.camera

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.OutputStream
import java.nio.ByteBuffer

object ColorFormatConverter {

    /**
     * Disclosure: This code was generated using Gemini – 2.5 Flash.
     *
     * """
     * Write the most simple possible converter function for converting from YUV420 to NV21 in
     * Kotlin. As input you get an Android `Image`.
     * """
     */
    fun yuvToJpeg(image: Image, output: OutputStream) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = yPlane.buffer.remaining()
        val vSize = yPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        // TODO: This line does not consider the case that the row stride of the Y plane differs
        //       from its width, i.e. there's padding at the end of each row and the data is not
        //       contiguous.
        yPlane.buffer.get(nv21, 0, ySize)

        val uvBuffer = ByteBuffer.wrap(nv21, ySize, nv21.size - ySize)

        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        uBuffer.position(0)
        vBuffer.position(0)

        val uvPixelStride = uPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride

        var uIndex = 0
        var vIndex = 0
        for (i in 0 until image.height / 2) {
            for (j in 0 until image.width / 2) {
                uvBuffer.put(vBuffer.get(vIndex))
                uvBuffer.put(uBuffer.get(uIndex))
                uIndex += uvPixelStride
                vIndex += uvPixelStride
            }
            uIndex += uRowStride - (image.width / 2) * uvPixelStride
            vIndex += vRowStride - (image.width / 2) * uvPixelStride
        }


        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null,
        )
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 95, output)
    }

}