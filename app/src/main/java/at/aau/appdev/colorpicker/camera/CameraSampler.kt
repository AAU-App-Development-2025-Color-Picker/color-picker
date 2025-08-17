package at.aau.appdev.colorpicker.camera

import android.opengl.GLES11Ext
import android.opengl.GLES30
import at.aau.appdev.colorpicker.camera.OpenGLUtility.checkProgramLinkStatus
import at.aau.appdev.colorpicker.camera.OpenGLUtility.checkProgramValidateStatus
import at.aau.appdev.colorpicker.camera.OpenGLUtility.checkShaderCompileStatus
import at.aau.appdev.colorpicker.camera.OpenGLUtility.floatBufferOf
import java.nio.ByteBuffer

object CameraSampler {

    private var vertShaderId = 1
    private var fragShaderId = 1
    private var programId = 1

    private val vertexArrayIds = IntArray(1)
    private val vertexBufferIds = IntArray(1)
    private val textureBufferIds = IntArray(1)
    private val frameBufferIds = IntArray(1)
    private var textureId = 1

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

    fun onSurfaceCreated() {
        compileShaders()
        linkProgram()
        generateBuffers()
        generateAttributes()
        generateFrameBuffer()
    }

    private fun compileShaders() {
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
                uniform vec2 u_SampleCoord;
                
                in vec2 v_TexCoord;
                
                out vec4 outColor;
                
                void main() {
                    outColor = texture(u_Texture, u_SampleCoord);
                }
            """.trimIndent()
        )
        GLES30.glCompileShader(fragShaderId)
        checkShaderCompileStatus(fragShaderId, "GLES30.GL_FRAGMENT_SHADER")
    }

    private fun linkProgram() {
        // PROGRAM:
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertShaderId)
        GLES30.glAttachShader(programId, fragShaderId)
        GLES30.glLinkProgram(programId)
        checkProgramLinkStatus(programId)
        checkProgramValidateStatus(programId)
    }

    private fun generateBuffers() {
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

    private fun generateAttributes() {
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

    private fun generateFrameBuffer() {
        // OUTPUT TEXTURE:
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
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

        // FRAME BUFFER:
        GLES30.glGenFramebuffers(1, frameBufferIds, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferIds[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0
        )

        // Unbind:
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun samplePixel(
        cameraTextureId: Int, px: Int, py: Int, cameraWidth: Int, cameraHeight: Int
    ): List<Int> {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferIds[0])
        GLES30.glViewport(0, 0, 1, 1)

        GLES30.glUseProgram(programId)

        val uTextureLocation = GLES30.glGetUniformLocation(programId, "u_Texture")
        GLES30.glUniform1i(uTextureLocation, 0)

        val uSampleLoc = GLES30.glGetUniformLocation(programId, "u_SampleCoord")
        val u = px.toFloat() / cameraWidth.toFloat()
        val v = py.toFloat() / cameraHeight.toFloat()
        GLES30.glUniform2f(uSampleLoc, u, v)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)

        GLES30.glBindVertexArray(vertexArrayIds[0])

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        val buffer = ByteBuffer.allocateDirect(4)
        GLES30.glReadPixels(0, 0, 1, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)

        // Unbind:
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        return listOf(
            // TODO: The 'and' operation here is not immediately obvious.
            //       I guess it's there to account for signed integer representation?
            buffer.get(0).toInt() and 0xFF,
            buffer.get(1).toInt() and 0xFF,
            buffer.get(2).toInt() and 0xFF,
            buffer.get(3).toInt() and 0xFF
        )
    }

}