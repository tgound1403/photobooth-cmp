package com.example.cameraxapp.core.render

import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** GL helpers shared by [GlRenderer] (offscreen bitmaps) and [FilterSurfaceProcessor] (live frames). */
internal object GlUtil {

    fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw IllegalStateException("GL program link failed: $log")
        }
        return program
    }

    fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw IllegalStateException("GL shader compile failed: $log")
        }
        return shader
    }

    fun checkGl(where: String) {
        val error = GLES30.glGetError()
        check(error == GLES30.GL_NO_ERROR) {
            "GL error 0x${Integer.toHexString(error)} during $where"
        }
    }

    /** A GL_TEXTURE_2D with linear filtering and clamp-to-edge wrapping. Storage is not allocated. */
    fun newTexture(target: Int = GLES30.GL_TEXTURE_2D): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(target, ids[0])
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    /** External OES texture that a SurfaceTexture writes camera frames into. */
    fun newOesTexture(): Int = newTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

    /** RGBA8 framebuffer of [w]x[h] with its colour texture. */
    fun createTarget(w: Int, h: Int): RenderTarget {
        val tex = newTexture()
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null,
        )
        val fbo = IntArray(1)
        GLES30.glGenFramebuffers(1, fbo, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex, 0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "Framebuffer incomplete (0x${Integer.toHexString(status)}) for ${w}x$h"
        }
        return RenderTarget(tex, fbo[0])
    }

    /** Full-screen quad: (x, y, u, v) per vertex, triangle strip. Returns the VAO and VBO ids. */
    fun createQuad(): Pair<Int, Int> {
        val vertices = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
        )
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)

        val vao = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glBindVertexArray(vao[0])
        val vbo = IntArray(1)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
        return vao[0] to vbo[0]
    }

    /**
     * Sets the uniforms of the adjust pass (see ADJUST_FRAGMENT_SHADER) from [p].
     * Texture units: uSrc = 0, uLut = 1. [useLut] false disables the LUT lookup.
     */
    fun applyAdjustUniforms(program: Int, p: EditParams, useLut: Boolean) {
        val (wr, wg, wb) = whiteBalanceGains(p.temperature, p.tint)
        setUniform1i(program, "uSrc", 0)
        setUniform1i(program, "uLut", 1)
        setUniform1i(program, "uUseLut", if (useLut) 1 else 0)
        setUniform1f(program, "uFilterIntensity", p.filterIntensity)
        setUniform1f(program, "uExposureGain", exposureGain(p.exposure))
        setUniform3f(program, "uWbGain", wr, wg, wb)
        setUniform1f(program, "uContrastFactor", contrastFactor(p.contrast))
        setUniform1f(program, "uSaturationFactor", saturationFactor(p.saturation))
        setUniform1f(program, "uToneAmount", TONE_AMPLITUDE)
        setUniform1f(program, "uHighlight", p.highlight)
        setUniform1f(program, "uShadow", p.shadow)
        setUniform1f(program, "uFadeAmount", fadeAmount(p.fade))
        setUniform1f(program, "uGrainAmplitude", grainAmplitude(p.grain))
        setUniform1f(program, "uVignetteStrength", vignetteStrength(p.vignette))
    }

    fun setUniform1i(program: Int, name: String, value: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), value)
    }

    fun setUniform1f(program: Int, name: String, value: Float) {
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, name), value)
    }

    fun setUniform2f(program: Int, name: String, x: Float, y: Float) {
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, name), x, y)
    }

    fun setUniform3f(program: Int, name: String, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(GLES30.glGetUniformLocation(program, name), x, y, z)
    }

    fun setUniformMatrix4(program: Int, name: String, matrix: FloatArray) {
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, name), 1, false, matrix, 0)
    }
}

/** A framebuffer and its colour texture. */
internal class RenderTarget(val texture: Int, val framebuffer: Int) {
    fun delete() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
    }
}
