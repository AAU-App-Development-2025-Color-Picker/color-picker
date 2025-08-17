package at.aau.appdev.colorpicker.camera

import android.opengl.GLES30

object OpenGLUtility {

    fun floatBufferOf(vararg floats: Float): java.nio.FloatBuffer {
        return java.nio.ByteBuffer.allocateDirect(floats.size * Float.SIZE_BYTES)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floats)
                position(0)
            }
    }

    fun checkShaderCompileStatus(shaderId: Int, shaderType: String) {
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