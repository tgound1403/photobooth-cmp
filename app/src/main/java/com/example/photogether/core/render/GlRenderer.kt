package com.example.photogether.core.render

import android.content.res.AssetManager
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
import com.example.photogether.core.render.GlUtil.buildProgram
import com.example.photogether.core.render.GlUtil.checkGl
import com.example.photogether.core.render.GlUtil.createTarget
import com.example.photogether.core.render.GlUtil.newTexture
import com.example.photogether.core.render.GlUtil.setUniform1f
import com.example.photogether.core.render.GlUtil.setUniform1i
import com.example.photogether.core.render.GlUtil.setUniform2f
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
class GlRenderer(private val assets: AssetManager? = null) : AutoCloseable {

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
    private val bloomPass = BloomPass()
    private var fxTextures: FxTextures? = null
    private var bloomTargets: BloomTargets? = null

    // Reused readback resources. Only touched on the GL thread; released in releaseGl().
    private var readBuffer: ByteBuffer? = null
    private var reusedOutput: Bitmap? = null

    /**
     * Renders [src] with [params]. [lut] is the 512x512 filter LUT (null for no filter).
     * If [maxSize] is set, the longest side of the output is scaled down to it (never upscaled).
     * When the params are identity and [maxSize] is null, returns a copy of [src] without using GL.
     *
     * Downscaling to [maxSize] happens on the GPU (the first pass renders straight into a target of
     * the output size). With [reuseOutput] the returned bitmap is owned by this renderer and is
     * reused by the next [render] call with the same output size: the caller must not recycle it
     * and must be done with it (or copy it) before rendering again or calling [close]. The default
     * returns a fresh bitmap that the caller owns.
     */
    suspend fun render(
        src: Bitmap,
        params: EditParams,
        lut: Bitmap?,
        maxSize: Int? = null,
        reuseOutput: Boolean = false,
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
        return runOnGlThread { renderOnGlThread(src, p, activeLut, maxSize, reuseOutput) }
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

    private fun renderOnGlThread(
        src: Bitmap,
        p: EditParams,
        lut: Bitmap?,
        maxSize: Int?,
        reuseOutput: Boolean,
    ): Bitmap {
        ensureGl()

        val limit = minOf(maxSize ?: Int.MAX_VALUE, maxTextureSize)
        val (outW, outH) = scaledSize(src.width, src.height, limit)
        val input = prepareInput(src)
        try {
            val srcTex = uploadBitmap(input, downscale = outW < input.width || outH < input.height)
            val lutTex = lut?.let { lutTextureFor(it) } ?: 0
            var first: RenderTarget? = null
            var second: RenderTarget? = null
            try {
                // The adjust pass samples the full-size source and writes at output size: the GPU
                // does the downscale (mipmapped linear filtering) instead of the CPU.
                first = createTarget(outW, outH)
                if (p.sharpness > 0f) second = createTarget(outW, outH)
                drawAdjust(p, srcTex, lutTex, first, outW, outH)
                val useBloom = BloomMath.bloomScale(p.halation) > 0f
                var bloomTex = 0
                if (useBloom) {
                    val targets = bloomTargetsFor(outW, outH)
                    bloomPass.run(first.texture, outW, outH, p.halation, targets, quadVao)
                    checkGl("bloom pass")
                    bloomTex = targets.result
                }
                val fx = FxValues(p, p.fxSeed)
                val useFx = fx.leak > 0f || fx.dust > 0f
                // The present/composite pass is needed for sharpening, the bloom composite or an overlay.
                if (second == null && (useBloom || useFx)) second = createTarget(outW, outH)
                val final =
                    if (second != null) {
                        drawSharpen(first.texture, second, p.sharpness, outW, outH, bloomTex, fx)
                        second
                    } else {
                        first
                    }
                return readBack(final, outW, outH, reuseOutput)
            } finally {
                second?.delete()
                first?.delete()
                GLES30.glDeleteTextures(1, intArrayOf(srcTex), 0)
            }
        } finally {
            if (input !== src) input.recycle()
        }
    }

    /**
     * Returns [src] if it can be uploaded as is, otherwise an ARGB_8888 copy (non-ARGB or hardware
     * bitmaps) that fits the GL texture size limit. The copy is owned by the caller to recycle.
     */
    private fun prepareInput(src: Bitmap): Bitmap {
        var cur = src
        if (cur.config != Bitmap.Config.ARGB_8888) {
            cur = cur.copy(Bitmap.Config.ARGB_8888, false)
                ?: error("Cannot convert source bitmap to ARGB_8888")
        }
        if (cur.width > maxTextureSize || cur.height > maxTextureSize) {
            // Rare: source larger than the GPU texture limit. Fit it on the CPU once.
            val (w, h) = scaledSize(cur.width, cur.height, maxTextureSize)
            val fitted = Bitmap.createScaledBitmap(cur, w, h, true)
            if (cur !== src && fitted !== cur) cur.recycle()
            cur = fitted
        }
        return cur
    }

    private fun uploadBitmap(bitmap: Bitmap, downscale: Boolean): Int {
        val tex = newTexture()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        if (downscale) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR,
            )
        }
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
        val tex = GlUtil.uploadLut3D(lut)
        lutTexture = tex
        lutSource = lut
        return tex
    }

    private fun drawAdjust(p: EditParams, srcTex: Int, lutTex: Int, target: RenderTarget, w: Int, h: Int) {
        bindTarget(target, w, h)
        GLES30.glUseProgram(adjustProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex)

        GlUtil.applyAdjustUniforms(adjustProgram, p, useLut = lutTex != 0, outputHeight = h)

        drawQuad()
        checkGl("adjust pass")
    }

    /** Small bloom FBOs, allocated once per output size and reused by later renders. */
    private fun bloomTargetsFor(w: Int, h: Int): BloomTargets {
        val cur = bloomTargets
        if (cur != null && cur.width == BloomMath.bloomSize(w) && cur.height == BloomMath.bloomSize(h)) return cur
        cur?.delete()
        bloomTargets = null
        return BloomTargets(w, h).also { bloomTargets = it }
    }

    private fun drawSharpen(srcTex: Int, target: RenderTarget, sharpness: Float, w: Int, h: Int, bloomTex: Int, fx: FxValues) {
        bindTarget(target, w, h)
        GLES30.glUseProgram(sharpenProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
        setUniform1i(sharpenProgram, "uSrc", 0)
        setUniform2f(sharpenProgram, "uTexel", 1f / w, 1f / h)
        setUniform1f(sharpenProgram, "uSharpness", sharpness)
        setUniform1i(sharpenProgram, "uBloom", 1)
        setUniform1i(sharpenProgram, "uUseBloom", if (bloomTex != 0) 1 else 0)
        if (bloomTex != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        }
        setUniform1i(sharpenProgram, "uLeakTex", FxTextures.LEAK_UNIT)
        setUniform1i(sharpenProgram, "uDustTex", FxTextures.DUST_UNIT)
        fx.apply(SharpenLocations(sharpenProgram), fxTextures ?: FxTextures.NONE)
        checkGl("fx uniforms")
        drawQuad()
        checkGl("sharpen pass")
    }

    private fun readBack(target: RenderTarget, w: Int, h: Int, reuseOutput: Boolean): Bitmap {
        val needed = w * h * 4
        var buffer = readBuffer
        if (buffer == null || buffer.capacity() != needed) {
            buffer = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder())
            readBuffer = buffer
        }
        buffer.clear()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 1)
        GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("read pixels")
        val out =
            if (reuseOutput) {
                val old = reusedOutput
                if (old != null && !old.isRecycled && old.width == w && old.height == h) {
                    old
                } else {
                    old?.recycle()
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { reusedOutput = it }
                }
            } else {
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            }
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

    private fun ensureGl() {
        if (initialized) return
        try {
            initEgl()
            check(
                GLES30.glGetString(GLES30.GL_VERSION)?.contains("OpenGL ES 3") == true,
            ) { "GLES 3.0 context unavailable" }
            adjustProgram = buildProgram(VERTEX_SHADER, ADJUST_FRAGMENT_SHADER)
            sharpenProgram = buildProgram(VERTEX_SHADER, SHARPEN_FRAGMENT_SHADER)
            bloomPass.init()
            fxTextures = FxTextures.load(assets)
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
        val (vao, vbo) = GlUtil.createQuad()
        quadVao = vao
        quadVbo = vbo
    }

    private fun releaseGl() {
        readBuffer = null
        reusedOutput?.recycle()
        reusedOutput = null
        if (display != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)
            if (lutTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
            bloomTargets?.delete()
            bloomPass.release()
            fxTextures?.release()
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
        bloomTargets = null
        fxTextures = null
        adjustProgram = 0
        sharpenProgram = 0
        quadVbo = 0
        quadVao = 0
        initialized = false
    }
}
