package at.aau.appdev.colorpicker.camera

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import at.aau.appdev.colorpicker.camera.OpenGLUtility.compileShader
import at.aau.appdev.colorpicker.camera.OpenGLUtility.generateArray
import at.aau.appdev.colorpicker.camera.OpenGLUtility.generateAttribute
import at.aau.appdev.colorpicker.camera.OpenGLUtility.generateBuffer
import at.aau.appdev.colorpicker.camera.OpenGLUtility.linkProgram
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import java.nio.ByteBuffer

object ARCoreSampler {

    private var programId = -1
    private var sampleTextureId = -1
    private var vertexArrayId = -1
    private var frameBufferId = -1

    private val vertexCoordData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    )
    private val textureCoordData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f,
    )

    private val vertShader = """
        #version 300 es
        
        in vec4 a_Position;
        in vec2 a_TexCoord;
        
        out vec2 v_TexCoord;
        
        void main() {
            gl_Position = a_Position;
        }
    """.trimIndent()
    private val fragShader = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision mediump float;
        
        uniform samplerExternalOES u_Texture;
        uniform vec2 u_SampleCoord;
        
        out vec4 outColor;
        
        void main() {
            outColor = texture(u_Texture, u_SampleCoord);
        }
    """.trimIndent()


    fun onSurfaceCreated() {
        val vertShaderId = compileShader(vertShader, GLES30.GL_VERTEX_SHADER)
        val fragShaderId = compileShader(fragShader, GLES30.GL_FRAGMENT_SHADER)
        this.programId = linkProgram(vertShaderId, fragShaderId)

        this.vertexArrayId = generateArray()
        val vertexBufferId = generateBuffer(vertexCoordData)
        generateAttribute(programId, vertexBufferId, "a_Position")

        this.sampleTextureId = generateSampleTexture()

        this.frameBufferId = generateFrameBuffer()
    }

    private fun generateSampleTexture(): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        val sampleTextureId = textureIds[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampleTextureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            1,
            1,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            null
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST
        )

        return sampleTextureId
    }

    private fun generateFrameBuffer(): Int {
        val frameBufferIds = IntArray(1)
        GLES30.glGenFramebuffers(1, frameBufferIds, 0)
        val frameBufferId = frameBufferIds[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            sampleTextureId,
            0
        )

        return frameBufferId
    }

    fun projectPointsAndSampleColors(
        consumeAnchors: () -> List<Anchor>, producePoints: (List<Point>) -> Unit
    ): (Frame, Int) -> Unit {
        return { frame, cameraTextureId ->
            projectPointsAndSampleColors(frame, cameraTextureId, consumeAnchors, producePoints)
        }
    }

    fun projectPointsAndSampleColors(
        frame: Frame,
        cameraTextureId: Int,
        consumeAnchors: () -> List<Anchor>,
        producePoints: (List<Point>) -> Unit
    ) {
        val anchors = consumeAnchors()
        if (anchors.isEmpty()) return

        // FIXME: Careful here!
        //        It is possible to provoke an exception by tapping in the 'CameraView' really fast.
        /*
            FATAL EXCEPTION: GLThread 70 (Ask Gemini)
            Process: at.aau.appdev.colorpicker, PID: 12997
            java.util.ConcurrentModificationException
                at java.util.ArrayList$Itr.next(ArrayList.java:860)
                at at.aau.appdev.colorpicker.camera.ARCoreSampler.projectAnchors(ARCoreSampler.kt:163)
                at at.aau.appdev.colorpicker.camera.ARCoreSampler.projectPointsAndSampleColors(ARCoreSampler.kt:137)
                at at.aau.appdev.colorpicker.camera.ARCoreSampler.projectPointsAndSampleColors$lambda$0(ARCoreSampler.kt:124)
                at at.aau.appdev.colorpicker.camera.ARCoreSampler.$r8$lambda$AhtYGBn2DKIeDBlv4AawdRgy6Qc(Unknown Source:0)
                at at.aau.appdev.colorpicker.camera.ARCoreSampler$$ExternalSyntheticLambda0.invoke(D8$$SyntheticClass:0)
                at at.aau.appdev.colorpicker.camera.CameraRenderer.onDrawFrame(CameraRenderer.kt:137)
                at android.opengl.GLSurfaceView$GLThread.guardedRun(GLSurfaceView.java:1573)
                at android.opengl.GLSurfaceView$GLThread.run(GLSurfaceView.java:1272)
         */
        val coordinates = projectAnchors(frame, anchors)
        val points = samplePixels(frame, cameraTextureId, coordinates)

        producePoints(points)
    }

    // https://learnopengl.com/getting-started/transformations
    // https://learnopengl.com/getting-started/coordinate-systems
    fun projectAnchors(/* display: Display, */ frame: Frame, anchors: List<Anchor>
    ): List<Pair<Float, Float>> {
        val projMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val cameraMatrix = FloatArray(16)

        val cameraTextureWidth = frame.camera.textureIntrinsics.imageDimensions[0]
        val cameraTextureHeight = frame.camera.textureIntrinsics.imageDimensions[1]

        frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f)
        frame.camera.getViewMatrix(viewMatrix, 0)

        Matrix.multiplyMM(cameraMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val screenPoints = mutableListOf<Pair<Float, Float>>()
        for (anchor in anchors) {
            val worldOrigin = floatArrayOf(0f, 0f, 0f, 1f)
            val worldMatrix = FloatArray(16)

            anchor.pose.toMatrix(worldMatrix, 0)

            val worldCoords = FloatArray(4)
            Matrix.multiplyMV(worldCoords, 0, worldMatrix, 0, worldOrigin, 0)

            val clipCoords = FloatArray(4)
            Matrix.multiplyMV(clipCoords, 0, cameraMatrix, 0, worldCoords, 0)

            if (clipCoords[3] <= 0f) {
                continue
            }
            val normalizedX = clipCoords[0] / clipCoords[3]
            val normalizedY = clipCoords[1] / clipCoords[3]

            // FIXME: Is it conceptually right to use the texture dimensions instead of the display
            //        dimensions? This depends on whether our overlay takes in ABSOLUTE or RELATIVE
            //        coordinates at which the color points should be drawn!
            //        ABSOLUTE -> Use display.
            //        RELATIVE -> Use texture.
            val screenX = (normalizedX * 0.5f + 0.5f) * cameraTextureWidth
            val screenY = (1f - (normalizedY * 0.5f + 0.5f)) * cameraTextureHeight

            screenPoints.add(Pair(screenX, screenY))
        }

        return screenPoints
    }

    // FIXME: It's rather ugly that the function called 'samplePixels' returns full-on points.
    //        Originally, it returned colors - but that wasn't too useful either. Rethink!
    fun samplePixels(
        frame: Frame, cameraTextureId: Int, points: List<Pair<Float, Float>>
    ): List<Point> {
        val cameraTextureWidth = frame.camera.textureIntrinsics.imageDimensions[0]
        val cameraTextureHeight = frame.camera.textureIntrinsics.imageDimensions[1]

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLES30.glViewport(0, 0, 1, 1)

        GLES30.glUseProgram(programId)

        val uTextureLocation = GLES30.glGetUniformLocation(programId, "u_Texture")
        GLES30.glUniform1i(uTextureLocation, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)

        GLES30.glBindVertexArray(vertexArrayId)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        val screenPoints = mutableListOf<Point>()

        for (point in points) {
            val x = point.first
            val y = point.second

            val uSampleLocation = GLES30.glGetUniformLocation(programId, "u_SampleCoord")
            val u = x / cameraTextureWidth.toFloat()
            val v = y / cameraTextureHeight.toFloat()
            GLES30.glUniform2f(uSampleLocation, u, v)

            val buffer = ByteBuffer.allocateDirect(4)
            GLES30.glReadPixels(0, 0, 1, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)

            screenPoints.add(
                Point(
                    Color(
                        red = buffer.get(0).toInt(),
                        green = buffer.get(1).toInt(),
                        blue = buffer.get(2).toInt(),
                        alpha = buffer.get(3).toInt()
                    ), Pair(x, y)
                )
            )
        }

        return screenPoints
    }

}