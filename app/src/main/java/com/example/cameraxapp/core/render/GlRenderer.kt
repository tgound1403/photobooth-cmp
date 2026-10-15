package com.example.cameraxapp.core.render

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Applies [EditParams] (LUT filter + global adjustments, optional sharpening) to a bitmap on the GPU.
 *
 * Runs on an OpenGL ES 3.0 context owned by a dedicated thread. The context is created lazily on the
 * first [render] call. All GL calls happen on that thread. Output is ARGB_8888 with the same
 * orientation as the input.
 *
 * Throws [IllegalStateException] if EGL or GLES 3.0 is unavailable, or if a GL call fails.
 */
class GlRenderer : AutoCloseable {

    private val thread = HandlerThread("GlRenderer").apply { start() }
    private val handler = Handler(thread.looper)

    @Volatile
    private var closed = false

    // GL state. Only touched on the GL thread.
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var initialized = false
    private var adjustProgram = 0
    private var sharpenProgram = 0
    private var quadVao = 0
    private var quadVbo = 0
    private var maxTextureSize = 0
    private var lutTexture = 0
    private var lutSource: Bitmap? = null

    /**
     * Renders [src] with [params]. [lut] is the 512x512 filter LUT (null for no filter).
     * If [maxSize] is set, the longest side of the output is scaled down to it (never upscaled).
     * When the params are identity and [maxSize] is null, returns a copy of [src] without using GL.
     */
    suspend fun render(
        src: Bitmap,
        params: EditParams,
        lut: Bitmap?,
        maxSize: Int? = null,
    ): Bitmap {
        require(!src.isRecycled) { "Source bitmap is recycled" }
        require(maxSize == null || maxSize > 0) { "maxSize must be positive, was $maxSize" }
        val p = params.clamped()
        if (p.isIdentity && maxSize == null) {
            return src.copy(Bitmap.Config.ARGB_8888, false)
        }
        val activeLut =
            lut?.takeIf { p.filterId != FilterCatalog.ORIGINAL_ID && p.filterIntensity > 0f }
        if (activeLut != null) {
            require(activeLut.width == LUT_SIZE && activeLut.height == LUT_SIZE) {
                "LUT must be ${LUT_SIZE}x$LUT_SIZE, was ${activeLut.width}x${activeLut.height}"
            }
        }
        return runOnGlThread { renderOnGlThread(src, p, activeLut, maxSize) }
    }

    override fun close() {
        if (closed) return
        closed = true
        handler.post {
            releaseGl()
            thread.quitSafely()
        }
    }

    private suspend fun <T> runOnGlThread(block: () -> T): T =
        suspendCancellableCoroutine<T> { cont ->
            val posted =
                handler.post {
                    val result =
                        runCatching {
                            check(!closed) { "GlRenderer is closed" }
                            block()
                        }
                    cont.resumeWith(result)
                }
            if (!posted) {
                cont.resumeWithException(IllegalStateException("GlRenderer is closed"))
            }
        }

    // ---- GL thread ---------------------------------------------------------------------------

    private fun renderOnGlThread(src: Bitmap, p: EditParams, lut: Bitmap?, maxSize: Int?): Bitmap {
        ensureGl()

        val limit = minOf(maxSize ?: Int.MAX_VALUE, maxTextureSize)
        val (outW, outH) = scaledSize(src.width, src.height, limit)
        val input = prepareInput(src, outW, outH)
        try {
            val srcTex = uploadBitmap(input)
            val lutTex = lut?.let { lutTextureFor(it) } ?: 0
            val first = createTarget(outW, outH)
            val second = if (p.sharpness > 0f) createTarget(outW, outH) else null
            try {
                drawAdjust(p, srcTex, lutTex, first, outW, outH)
                val final =
                    if (second != null) {
                        drawSharpen(first.texture, second, p.sharpness, outW, outH)
                        second
                    } else {
                        first
                    }
                return readBack(final, outW, outH)
            } finally {
                second?.delete()
                first.delete()
                GLES30.glDeleteTextures(1, intArrayOf(srcTex), 0)
            }
        } finally {
            if (input !== src) input.recycle()
        }
    }

    /** Scales [src] to [w]x[h] with filtering (CPU, once) and makes it ARGB_8888 for upload. */
    private fun prepareInput(src: Bitmap, w: Int, h: Int): Bitmap {
        val sized =
            if (w == src.width && h == src.height) src
            else Bitmap.createScaledBitmap(src, w, h, true)
        if (sized.config == Bitmap.Config.ARGB_8888) return sized
        val converted = sized.copy(Bitmap.Config.ARGB_8888, false)
        if (sized !== src) sized.recycle()
        return converted
    }

    private fun uploadBitmap(bitmap: Bitmap): Int {
        val tex = newTexture()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        checkGl("upload source texture")
        return tex
    }

    /** Uploads the LUT once and reuses the texture while the same bitmap is passed in. */
    private fun lutTextureFor(lut: Bitmap): Int {
        if (lut === lutSource && lutTexture != 0) return lutTexture
        if (lutTexture != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
            lutTexture = 0
        }
        lutSource = null
        val tex = newTexture()
        val argb = if (lut.config == Bitmap.Config.ARGB_8888) lut else lut.copy(Bitmap.Config.ARGB_8888, false)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, argb, 0)
        if (argb !== lut) argb.recycle()
        checkGl("upload LUT texture")
        lutTexture = tex
        lutSource = lut
        return tex
    }

    private fun drawAdjust(p: EditParams, srcTex: Int, lutTex: Int, target: RenderTarget, w: Int, h: Int) {
        val (wr, wg, wb) = whiteBalanceGains(p.temperature, p.tint)
        bindTarget(target, w, h)
        GLES30.glUseProgram(adjustProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (lutTex != 0) lutTex else 0)

        setUniform1i(adjustProgram, "uSrc", 0)
        setUniform1i(adjustProgram, "uLut", 1)
        setUniform1i(adjustProgram, "uUseLut", if (lutTex != 0) 1 else 0)
        setUniform1f(adjustProgram, "uFilterIntensity", p.filterIntensity)
        setUniform1f(adjustProgram, "uExposureGain", exposureGain(p.exposure))
        setUniform3f(adjustProgram, "uWbGain", wr, wg, wb)
        setUniform1f(adjustProgram, "uContrastFactor", contrastFactor(p.contrast))
        setUniform1f(adjustProgram, "uSaturationFactor", saturationFactor(p.saturation))
        setUniform1f(adjustProgram, "uToneAmount", TONE_AMPLITUDE)
        setUniform1f(adjustProgram, "uHighlight", p.highlight)
        setUniform1f(adjustProgram, "uShadow", p.shadow)
        setUniform1f(adjustProgram, "uFadeAmount", fadeAmount(p.fade))
        setUniform1f(adjustProgram, "uGrainAmplitude", grainAmplitude(p.grain))
        setUniform1f(adjustProgram, "uVignetteStrength", vignetteStrength(p.vignette))

        drawQuad()
        checkGl("adjust pass")
    }

    private fun drawSharpen(srcTex: Int, target: RenderTarget, sharpness: Float, w: Int, h: Int) {
        bindTarget(target, w, h)
        GLES30.glUseProgram(sharpenProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
        setUniform1i(sharpenProgram, "uSrc", 0)
        setUniform2f(sharpenProgram, "uTexel", 1f / w, 1f / h)
        setUniform1f(sharpenProgram, "uSharpness", sharpness)
        drawQuad()
        checkGl("sharpen pass")
    }

    private fun readBack(target: RenderTarget, w: Int, h: Int): Bitmap {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
        val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 1)
        GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        checkGl("read pixels")
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        out.copyPixelsFromBuffer(buffer)
        return out
    }

    private fun bindTarget(target: RenderTarget, w: Int, h: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
        GLES30.glViewport(0, 0, w, h)
    }

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun createTarget(w: Int, h: Int): RenderTarget {
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

    private fun newTexture(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun setUniform1i(program: Int, name: String, value: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun setUniform1f(program: Int, name: String, value: Float) {
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun setUniform2f(program: Int, name: String, x: Float, y: Float) {
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, name), x, y)
    }

    private fun setUniform3f(program: Int, name: String, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(GLES30.glGetUniformLocation(program, name), x, y, z)
    }

    private fun ensureGl() {
        if (initialized) return
        try {
            initEgl()
            check(
                GLES30.glGetString(GLES30.GL_VERSION)?.contains("OpenGL ES 3") == true,
            ) { "GLES 3.0 context unavailable" }
            adjustProgram = buildProgram(VERTEX_SHADER, ADJUST_FRAGMENT_SHADER)
            sharpenProgram = buildProgram(VERTEX_SHADER, SHARPEN_FRAGMENT_SHADER)
            setupQuad()
            val max = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, max, 0)
            maxTextureSize = max[0]
            checkGl("initialisation")
            initialized = true
        } catch (t: Throwable) {
            releaseGl()
            throw if (t is IllegalStateException) t else IllegalStateException("GlRenderer init failed: ${t.message}", t)
        }
    }

    private fun initEgl() {
        val disp = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(disp != EGL14.EGL_NO_DISPLAY) { "EGL: no default display" }
        display = disp
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) {
            "EGL: eglInitialize failed (0x${Integer.toHexString(EGL14.eglGetError())})"
        }

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(
            EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                numConfigs[0] > 0 && configs[0] != null,
        ) { "EGL: no OpenGL ES 3.0 config available on this device" }
        val config = configs[0]!!

        eglContext = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0,
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) {
            "EGL: cannot create OpenGL ES 3.0 context (0x${Integer.toHexString(EGL14.eglGetError())})"
        }
        eglSurface = EGL14.eglCreatePbufferSurface(
            display, config, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "EGL: cannot create pbuffer surface" }
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) {
            "EGL: eglMakeCurrent failed (0x${Integer.toHexString(EGL14.eglGetError())})"
        }
    }

    private fun setupQuad() {
        val vertices = floatArrayOf(
            // x, y, u, v
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
        )
        val buffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)

        val vao = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        quadVao = vao[0]
        GLES30.glBindVertexArray(quadVao)

        val vbo = IntArray(1)
        GLES30.glGenBuffers(1, vbo, 0)
        quadVbo = vbo[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
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

    private fun compileShader(type: Int, source: String): Int {
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

    private fun checkGl(where: String) {
        val error = GLES30.glGetError()
        check(error == GLES30.GL_NO_ERROR) {
            "GL error 0x${Integer.toHexString(error)} during $where"
        }
    }

    private fun releaseGl() {
        if (display != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)
            if (lutTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
            if (adjustProgram != 0) GLES30.glDeleteProgram(adjustProgram)
            if (sharpenProgram != 0) GLES30.glDeleteProgram(sharpenProgram)
            if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
            if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
        display = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
        lutTexture = 0
        lutSource = null
        adjustProgram = 0
        sharpenProgram = 0
        quadVbo = 0
        quadVao = 0
        initialized = false
    }

    private class RenderTarget(val texture: Int, val framebuffer: Int) {
        fun delete() {
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
            GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
        }
    }
}
