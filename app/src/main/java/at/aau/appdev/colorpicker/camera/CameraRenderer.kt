package at.aau.appdev.colorpicker.camera

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Display
import at.aau.appdev.colorpicker.camera.OpenGLUtility.compileShaders
import at.aau.appdev.colorpicker.camera.OpenGLUtility.generateAttributes
import at.aau.appdev.colorpicker.camera.OpenGLUtility.generateBuffers
import at.aau.appdev.colorpicker.camera.OpenGLUtility.linkProgram
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class CameraRenderer(
    private val session: Session,
    private val display: Display?,
    private val handleInteraction: (Frame) -> Unit,
    private val updatePreview: (Frame, Int) -> Unit,
) : GLSurfaceView.Renderer {

    private var displayWidth = 1
    private var displayHeight = 1

    private var programId = -1
    private var cameraTextureId = -1
    private var vertexArrayId = -1

    private val vertexCoordData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    )
    private val textureCoordData = floatArrayOf(
        1.0f, 0.827124f,
        1.0f, 0.17287594f,
        0.0f, 0.827124f,
        0.0f, 0.17287594f,
    )

    private val vertShader = """
        #version 300 es
        
        in vec4 a_Position;
        in vec2 a_TexCoord;
        
        out vec2 v_TexCoord;
        
        void main() {
            gl_Position = a_Position;
            v_TexCoord = a_TexCoord;
        }
    """.trimIndent()
    private val fragShader = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision mediump float;
        
        uniform samplerExternalOES u_Texture;
        
        in vec2 v_TexCoord;
        
        out vec4 outColor;
        
        void main() {
            outColor = texture(u_Texture, v_TexCoord);
        }
    """.trimIndent()

    lateinit var frame: Frame


    override fun onSurfaceCreated(
        gl: GL10?, config: EGLConfig?
    ) {
        // Renderer setup:
        val (vertShaderId, fragShaderId) = compileShaders(vertShader, fragShader)
        this.programId = linkProgram(vertShaderId, fragShaderId)
        val (vertexBufferId, textureBufferId) = generateBuffers(vertexCoordData, textureCoordData)
        this.vertexArrayId = generateAttributes(programId, vertexBufferId, textureBufferId)
        this.cameraTextureId = generateTextures()
        session.setCameraTextureName(cameraTextureId)

        // Sampler setup:
        ARCoreSampler.onSurfaceCreated()
    }

    override fun onSurfaceChanged(
        gl: GL10?, width: Int, height: Int
    ) {
        this.displayWidth = width
        this.displayHeight = height
        setDisplayGeometry()
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            this.frame = session.update()

            handleInteraction(frame)

            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                vertexCoordData,
                Coordinates2d.TEXTURE_NORMALIZED,
                textureCoordData
            )

            GLES30.glClearColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

            GLES30.glUseProgram(programId)

            val uTextureLocation = GLES30.glGetUniformLocation(programId, "u_Texture")
            GLES30.glUniform1i(uTextureLocation, 0)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)

            GLES30.glBindVertexArray(vertexArrayId)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

            updatePreview(frame, cameraTextureId)
        } catch (e: CameraNotAvailableException) {
            Log.e(
                "GLSurfaceView.Renderer.onDrawFrame", "Camera not available during onDrawFrame", e
            )
        }
    }

    private fun generateTextures(): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        val cameraTextureId = textureIds[0]

        return cameraTextureId
    }

    private fun setDisplayGeometry() {
        if (display == null || this.displayWidth == 0 || this.displayHeight == 0) return
        val displayRotation = this.display.rotation
        session.setDisplayGeometry(
            displayRotation, this.displayWidth, this.displayHeight
        )
    }

}
