package com.example.photogether.core.render

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.example.photogether.core.render.GlUtil.buildProgram
import com.example.photogether.core.render.GlUtil.checkGl
import com.example.photogether.core.render.GlUtil.createQuad
import com.example.photogether.core.render.GlUtil.setUniform1f
import com.example.photogether.core.render.GlUtil.setUniform1i
import com.example.photogether.core.render.GlUtil.setUniform2f
import com.example.photogether.core.render.GlUtil.setUniformMatrix4
import java.util.concurrent.Executor

/**
 * CameraX [SurfaceProcessor] that applies [EditParams] (and the filter LUT) to every camera frame.
 *
 * Per camera frame, on the GL thread:
 *  1. The frame is latched from a SurfaceTexture (external OES texture). The adjust pass draws it
 *     into an intermediate RGBA texture, using the SurfaceTexture transform.
 *  2. For each output surface, the present pass samples the intermediate texture with the
 *     SurfaceOutput transform (rotation, mirroring) and applies the optional sharpen. Sharpness 0 is
 *     a plain copy. The result is swapped to the output's EGL window surface.
 *
 * The edit can be changed at any time with [setEdit]. The next frame uses it. The camera is not
 * rebound. Each output surface is closed through [SurfaceOutput.close] when CameraX requests it.
 *
 * Create one instance per camera session and call [release] when the session ends.
 */
class FilterSurfaceProcessor : SurfaceProcessor {

    private class Edit(val params: EditParams, val lut: Bitmap?)

    private val thread = HandlerThread("FilterSurfaceProcessor").apply { start() }
    private val handler = Handler(thread.looper)
    private val glExecutor = Executor { handler.post(it) }

    @Volatile
    private var edit = Edit(EditParams(), null)

    @Volatile
    private var released = false

    // GL state. Only touched on the handler thread.
    private var initialized = false
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglConfig: EGLConfig? = null
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var pbuffer: EGLSurface = EGL14.EGL_NO_SURFACE
    private var adjustProgram = 0
    private var sharpenProgram = 0
    private var quadVao = 0
    private var quadVbo = 0
    private var lutTexture = 0
    private var uploadedLut: Bitmap? = null
    // CameraX can ask for a new input surface while the previous request is still being torn down
    // (the preview pipeline is rebuilt when the surface provider is set). Every request therefore
    // owns its Input, and only its own release callback may free it. [input] is the newest one.
    private var input: Input? = null
    private val inputs = ArrayList<Input>()
    private val outputs = ArrayList<Output>()
    private val texMatrix = FloatArray(16)
    private val outMatrix = FloatArray(16)
    private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    /**
     * Sets the edit and the LUT used for the following frames. Safe to call from any thread.
     * [lut] may be null for no filter. It must be a 512x512 bitmap and is not recycled by this class.
     */
    fun setEdit(params: EditParams, lut: Bitmap?) {
        edit = Edit(params, lut)
    }

    /** Stops rendering, closes all outputs and releases the GL context. Idempotent. */
    fun release() {
        if (released) return
        released = true
        handler.post {
            releaseAll()
            thread.quitSafely()
        }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        handler.post {
            if (released) {
                request.willNotProvideSurface()
                return@post
            }
            var created: Input? = null
            try {
                ensureEgl()
                makeCurrent(pbuffer)
                val size = request.resolution
                val texture = GlUtil.newOesTexture()
                val surfaceTexture = SurfaceTexture(texture)
                surfaceTexture.setDefaultBufferSize(size.width, size.height)
                val surface = Surface(surfaceTexture)
                val inp = Input(texture, surfaceTexture, surface, size.width, size.height)
                created = inp
                inp.intermediate = GlUtil.createTarget(size.width, size.height)
                surfaceTexture.setOnFrameAvailableListener({ onFrameAvailable(inp) }, handler)
                inputs += inp
                input = inp
                request.provideSurface(surface, glExecutor) { releaseInput(inp) }
            } catch (t: Throwable) {
                Log.e(TAG, "Camera input setup failed; frames will not be filtered", t)
                created?.let { releaseInput(it) }
                request.willNotProvideSurface()
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        handler.post {
            try {
                ensureEgl()
                val surface = surfaceOutput.getSurface(glExecutor) { event ->
                    if (event.eventCode == SurfaceOutput.Event.EVENT_REQUEST_CLOSE) {
                        closeOutput(surfaceOutput)
                    }
                }
                val eglSurface = EGL14.eglCreateWindowSurface(
                    display, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0,
                )
                check(eglSurface != EGL14.EGL_NO_SURFACE) {
                    "eglCreateWindowSurface failed (0x${Integer.toHexString(EGL14.eglGetError())})"
                }
                outputs += Output(surfaceOutput, eglSurface)
            } catch (e: IllegalArgumentException) {
                // The preview surface was destroyed before this task ran (leaving the screen). Not an error.
                Log.w(TAG, "Output surface already gone; skipping it")
                closeOutput(surfaceOutput)
            } catch (t: Throwable) {
                Log.e(TAG, "Output surface setup failed", t)
                closeOutput(surfaceOutput)
            }
        }
    }

    // ---- handler thread -----------------------------------------------------------------------

    private fun onFrameAvailable(inp: Input) {
        if (inp.released || inp !== input) return
        val target = inp.intermediate ?: return
        val snapshot = edit
        val p = snapshot.params.clamped()
        val activeLut =
            snapshot.lut?.takeIf { p.filterId != FilterCatalog.ORIGINAL_ID && p.filterIntensity > 0f }
        try {
            makeCurrent(pbuffer)
            inp.surfaceTexture.updateTexImage()
            inp.surfaceTexture.getTransformMatrix(texMatrix)
            val timestamp = inp.surfaceTexture.timestamp
            val lutTex = activeLut?.let { lutTextureFor(it) } ?: 0

            // Pass 1: camera frame -> intermediate, in upright texture coordinates.
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
            GLES30.glViewport(0, 0, inp.width, inp.height)
            GLES30.glUseProgram(adjustProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inp.texture)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTex)
            setUniformMatrix4(adjustProgram, "uTexMatrix", texMatrix)
            GlUtil.applyAdjustUniforms(adjustProgram, p, useLut = lutTex != 0)
            drawQuad()
            checkGl("adjust pass")

            // Pass 2: intermediate -> each output surface.
            for (out in outputs.toList()) {
                presentTo(out, target.texture, inp.width, inp.height, p.sharpness, timestamp)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Frame render failed", t)
        }
    }

    private fun presentTo(
        out: Output,
        srcTexture: Int,
        srcWidth: Int,
        srcHeight: Int,
        sharpness: Float,
        timestampNs: Long,
    ) {
        try {
            makeCurrent(out.eglSurface)
            val size = out.surfaceOutput.size
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, size.width, size.height)
            out.surfaceOutput.updateTransformMatrix(outMatrix, identity)
            GLES30.glUseProgram(sharpenProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTexture)
            setUniform1i(sharpenProgram, "uSrc", 0)
            setUniform2f(sharpenProgram, "uTexel", 1f / srcWidth, 1f / srcHeight)
            setUniform1f(sharpenProgram, "uSharpness", sharpness)
            setUniformMatrix4(sharpenProgram, "uTexMatrix", outMatrix)
            drawQuad()
            checkGl("present pass")
            EGLExt.eglPresentationTimeANDROID(display, out.eglSurface, timestampNs)
            if (!EGL14.eglSwapBuffers(display, out.eglSurface)) {
                Log.w(TAG, "eglSwapBuffers failed (0x${Integer.toHexString(EGL14.eglGetError())}); closing output")
                closeOutput(out.surfaceOutput)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Output render failed; closing output", t)
            closeOutput(out.surfaceOutput)
        }
    }

    private fun closeOutput(surfaceOutput: SurfaceOutput) {
        val index = outputs.indexOfFirst { it.surfaceOutput === surfaceOutput }
        if (index >= 0) {
            val out = outputs.removeAt(index)
            if (display != EGL14.EGL_NO_DISPLAY && out.eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, out.eglSurface)
            }
        }
        surfaceOutput.close()
    }

    private fun releaseInput(inp: Input) {
        if (inp.released) return
        inp.released = true
        inputs.remove(inp)
        if (input === inp) input = inputs.lastOrNull()
        if (initialized) makeCurrent(pbuffer)
        inp.intermediate?.delete()
        inp.intermediate = null
        inp.surface.release()
        inp.surfaceTexture.release()
        if (initialized) GLES30.glDeleteTextures(1, intArrayOf(inp.texture), 0)
    }

    private fun lutTextureFor(bitmap: Bitmap): Int {
        if (bitmap === uploadedLut && lutTexture != 0) return lutTexture
        if (lutTexture != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
            lutTexture = 0
        }
        uploadedLut = null
        val tex = GlUtil.newTexture()
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, argb, 0)
        if (argb !== bitmap) argb.recycle()
        checkGl("upload LUT texture")
        lutTexture = tex
        uploadedLut = bitmap
        return tex
    }

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun makeCurrent(surface: EGLSurface) {
        check(EGL14.eglMakeCurrent(display, surface, surface, eglContext)) {
            "eglMakeCurrent failed (0x${Integer.toHexString(EGL14.eglGetError())})"
        }
    }

    private fun ensureEgl() {
        if (initialized) return
        try {
            val disp = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(disp != EGL14.EGL_NO_DISPLAY) { "EGL: no default display" }
            display = disp
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) {
                "EGL: eglInitialize failed (0x${Integer.toHexString(EGL14.eglGetError())})"
            }

            // Window + pbuffer, recordable so that encoder-backed surfaces (video) can be used.
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(
                EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                    numConfigs[0] > 0 && configs[0] != null,
            ) { "EGL: no OpenGL ES 3.0 window config available on this device" }
            eglConfig = configs[0]

            eglContext = EGL14.eglCreateContext(
                display, eglConfig, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0,
            )
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "EGL: cannot create OpenGL ES 3.0 context" }
            pbuffer = EGL14.eglCreatePbufferSurface(
                display, eglConfig, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
            )
            check(pbuffer != EGL14.EGL_NO_SURFACE) { "EGL: cannot create pbuffer surface" }
            makeCurrent(pbuffer)
            check(GLES30.glGetString(GLES30.GL_VERSION)?.contains("OpenGL ES 3") == true) {
                "GLES 3.0 context unavailable"
            }

            adjustProgram = buildProgram(VERTEX_SHADER_TEXMATRIX, ADJUST_FRAGMENT_SHADER_OES)
            sharpenProgram = buildProgram(VERTEX_SHADER_TEXMATRIX, SHARPEN_FRAGMENT_SHADER)
            val (vao, vbo) = createQuad()
            quadVao = vao
            quadVbo = vbo
            checkGl("initialisation")
            initialized = true
        } catch (t: Throwable) {
            releaseAll()
            throw if (t is IllegalStateException) t else IllegalStateException("FilterSurfaceProcessor init failed: ${t.message}", t)
        }
    }

    private fun releaseAll() {
        outputs.toList().forEach { closeOutput(it.surfaceOutput) }
        inputs.toList().forEach { releaseInput(it) }
        input = null
        if (initialized) {
            makeCurrent(pbuffer)
            if (lutTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
            if (adjustProgram != 0) GLES30.glDeleteProgram(adjustProgram)
            if (sharpenProgram != 0) GLES30.glDeleteProgram(sharpenProgram)
            if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
            if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (pbuffer != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, pbuffer)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
        display = EGL14.EGL_NO_DISPLAY
        eglConfig = null
        eglContext = EGL14.EGL_NO_CONTEXT
        pbuffer = EGL14.EGL_NO_SURFACE
        adjustProgram = 0
        sharpenProgram = 0
        quadVao = 0
        quadVbo = 0
        lutTexture = 0
        uploadedLut = null
        initialized = false
    }

    private class Input(
        val texture: Int,
        val surfaceTexture: SurfaceTexture,
        val surface: Surface,
        val width: Int,
        val height: Int,
    ) {
        var intermediate: RenderTarget? = null
        var released = false
    }

    private class Output(val surfaceOutput: SurfaceOutput, val eglSurface: EGLSurface)

    private companion object {
        const val TAG = "FilterSurfaceProcessor"
    }
}
