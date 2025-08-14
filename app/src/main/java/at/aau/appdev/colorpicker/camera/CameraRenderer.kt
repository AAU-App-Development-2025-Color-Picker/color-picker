package at.aau.appdev.colorpicker.camera

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Display
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
    private val handleInteraction: (Frame) -> Unit
) : GLSurfaceView.Renderer {

    private var displayWidth = 1
    private var displayHeight = 1

    private var textureId = 1
    private var vertShaderId = 1
    private var fragShaderId = 1
    private var programId = 1
    private var vertexArrayIds = IntArray(1)
    private var vertexBufferIds = IntArray(1)
    private var textureBufferIds = IntArray(1)

    private val vertexCoordData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    )
    private var textureCoordData = floatArrayOf(
        1.0f, 0.827124f,
        1.0f, 0.17287594f,
        0.0f, 0.827124f,
        0.0f, 0.17287594f,
    )

    lateinit var frame: Frame

    override fun onSurfaceCreated(
        gl: GL10?, config: EGLConfig?
    ) {
        fun compileShaders() {
            // VERTEX SHADER:
            vertShaderId = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
            GLES30.glShaderSource(
                vertShaderId, """
                    #version 300 es
                    
                    in vec4 a_Position;
                    in vec2 a_TexCoord;
                    
                    out vec2 v_TexCoord;
                    
                    void main() {
                        gl_Position = a_Position;
                        v_TexCoord = a_TexCoord;
                    }
                """.trimIndent()
            )
            GLES30.glCompileShader(vertShaderId)
            checkShaderCompileStatus(vertShaderId, "GLES30.GL_VERTEX_SHADER")

            // FRAGMENT SHADER:
            fragShaderId = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
            GLES30.glShaderSource(
                fragShaderId, """
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
            )
            GLES30.glCompileShader(fragShaderId)
            checkShaderCompileStatus(fragShaderId, "GLES30.GL_FRAGMENT_SHADER")
        }

        fun linkProgram() {
            // PROGRAM:
            programId = GLES30.glCreateProgram()
            GLES30.glAttachShader(programId, vertShaderId)
            GLES30.glAttachShader(programId, fragShaderId)
            GLES30.glLinkProgram(programId)
            checkProgramLinkStatus(programId)
            checkProgramValidateStatus(programId)
        }

        fun generateBuffers() {
            // VERTEX BUFFER:
            GLES30.glGenBuffers(1, vertexBufferIds, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferIds[0])
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                vertexCoordData.size * Float.SIZE_BYTES,
                floatBufferOf(*vertexCoordData),
                GLES30.GL_STATIC_DRAW
            )

            // TEXTURE BUFFER:
            GLES30.glGenBuffers(1, textureBufferIds, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textureBufferIds[0])
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                textureCoordData.size * Float.SIZE_BYTES,
                floatBufferOf(*textureCoordData),
                GLES30.GL_STATIC_DRAW
            )
        }

        fun generateAttributes() {
            GLES30.glGenVertexArrays(1, vertexArrayIds, 0)
            GLES30.glBindVertexArray(vertexArrayIds[0])

            // VERTEX SHADER:
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferIds[0])
            val posAttribLocation = GLES30.glGetAttribLocation(programId, "a_Position")
            GLES30.glEnableVertexAttribArray(posAttribLocation)
            GLES30.glVertexAttribPointer(posAttribLocation, 2, GLES30.GL_FLOAT, false, 0, 0)

            // FRAGMENT SHADER:
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textureBufferIds[0])
            val texAttribLocation = GLES30.glGetAttribLocation(programId, "a_TexCoord")
            GLES30.glEnableVertexAttribArray(texAttribLocation)
            GLES30.glVertexAttribPointer(texAttribLocation, 2, GLES30.GL_FLOAT, false, 0, 0)
        }

        fun generateTextures() {
            val textureIds = IntArray(1)
            GLES30.glGenTextures(1, textureIds, 0)
            textureId = textureIds[0]

            session.setCameraTextureName(textureId)
        }

        compileShaders()
        linkProgram()
        generateBuffers()
        generateAttributes()
        generateTextures()
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

            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                vertexCoordData,
                Coordinates2d.TEXTURE_NORMALIZED,
                textureCoordData
            )

            GLES30.glClearColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

            GLES30.glUseProgram(programId)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES30.glUseProgram(programId)
            val uTextureLocation = GLES30.glGetUniformLocation(programId, "u_Texture")
            GLES30.glUniform1i(uTextureLocation, 0)

            GLES30.glBindVertexArray(vertexArrayIds[0])

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

            // Unbind:
            GLES30.glBindVertexArray(0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        } catch (e: CameraNotAvailableException) {
            Log.e(
                "GLSurfaceView.Renderer.onDrawFrame", "Camera not available during onDrawFrame", e
            )
        }
    }

    private fun floatBufferOf(vararg floats: Float): java.nio.FloatBuffer {
        return java.nio.ByteBuffer.allocateDirect(floats.size * Float.SIZE_BYTES)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floats)
                position(0)
            }
    }

    private fun checkShaderCompileStatus(shaderId: Int, shaderType: String) {
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetShaderInfoLog(shaderId)
            throw RuntimeException("GLSL Shader compilation failed:\n[$shaderId - $shaderType] $log")
        }
    }

    private fun checkProgramLinkStatus(programId: Int) {
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetProgramInfoLog(programId)
            throw RuntimeException("GLSL Program linking failed:\n[$programId] $log")
        }
    }

    private fun checkProgramValidateStatus(programId: Int) {
        GLES30.glValidateProgram(programId)

        val validateStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_VALIDATE_STATUS, validateStatus, 0)
        if (validateStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetProgramInfoLog(programId)
            throw RuntimeException("GLSL Program validation failed:\n[$programId] $log")
        }
    }

    private fun setDisplayGeometry() {
        if (display == null || this.displayWidth == 0 || this.displayHeight == 0) return
        val displayRotation = this.display!!.rotation
        session.setDisplayGeometry(
            displayRotation, this.displayWidth, this.displayHeight
        )
    }

}
