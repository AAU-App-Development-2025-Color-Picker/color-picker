package at.aau.appdev.colorpicker.camera

import android.opengl.GLES30

object OpenGLUtility {

    fun compileShader(shader: String, shaderType: Int): Int {
        val shaderId = GLES30.glCreateShader(shaderType)
        GLES30.glShaderSource(
            shaderId, shader.trimIndent()
        )
        GLES30.glCompileShader(shaderId)
        checkShaderCompileStatus(shaderId, shaderType)

        return shaderId
    }

    fun linkProgram(vertShaderId: Int, fragShaderId: Int): Int {
        val programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertShaderId)
        GLES30.glAttachShader(programId, fragShaderId)
        GLES30.glLinkProgram(programId)
        checkProgramLinkStatus(programId)
        checkProgramValidateStatus(programId)

        return programId
    }

    fun generateBuffer(coordData: FloatArray): Int {
        val bufferIds = IntArray(1)
        GLES30.glGenBuffers(1, bufferIds, 0)
        val bufferId = bufferIds[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufferId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            coordData.size * Float.SIZE_BYTES,
            floatBufferOf(*coordData),
            GLES30.GL_STATIC_DRAW
        )

        return bufferId
    }

    fun generateArray(): Int {
        val vertexArrayIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vertexArrayIds, 0)
        val vertexArrayId = vertexArrayIds[0]
        GLES30.glBindVertexArray(vertexArrayId)

        return vertexArrayId
    }

    fun generateAttribute(programId: Int, bufferId: Int, attribute: String) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufferId)
        val posAttribLocation = GLES30.glGetAttribLocation(programId, attribute)
        GLES30.glEnableVertexAttribArray(posAttribLocation)
        GLES30.glVertexAttribPointer(posAttribLocation, 2, GLES30.GL_FLOAT, false, 0, 0)
    }

    fun floatBufferOf(vararg floats: Float): java.nio.FloatBuffer {
        return java.nio.ByteBuffer.allocateDirect(floats.size * Float.SIZE_BYTES)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floats)
                position(0)
            }
    }

    fun checkShaderCompileStatus(shaderId: Int, shaderType: Int) {
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetShaderInfoLog(shaderId)
            throw RuntimeException("GLSL Shader compilation failed:\n[$shaderId - $shaderType] $log")
        }
    }

    fun checkProgramLinkStatus(programId: Int) {
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetProgramInfoLog(programId)
            throw RuntimeException("GLSL Program linking failed:\n[$programId] $log")
        }
    }

    fun checkProgramValidateStatus(programId: Int) {
        GLES30.glValidateProgram(programId)

        val validateStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_VALIDATE_STATUS, validateStatus, 0)
        if (validateStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetProgramInfoLog(programId)
            throw RuntimeException("GLSL Program validation failed:\n[$programId] $log")
        }
    }

}