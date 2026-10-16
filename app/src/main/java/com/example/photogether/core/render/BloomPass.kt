package com.example.photogether.core.render

import android.opengl.GLES30
import com.example.photogether.core.render.GlUtil.buildProgram
import com.tgound.photogether.BuildConfig

/** The two quarter-size ping-pong targets of the bloom pass. Allocated once per input size. */
internal class BloomTargets(fullWidth: Int, fullHeight: Int) {
    val width = BloomMath.bloomSize(fullWidth)
    val height = BloomMath.bloomSize(fullHeight)
    val a: RenderTarget = GlUtil.createTarget(width, height)
    val b: RenderTarget = try {
        GlUtil.createTarget(width, height)
    } catch (t: Throwable) {
        a.delete()
        throw t
    }

    /** The finished, tinted and scaled bloom (the vertical blur writes into [a]). */
    val result: Int get() = a.texture

    fun delete() {
        a.delete()
        b.delete()
    }
}

/**
 * Halation/bloom: downsample + threshold, then a separable 9-tap Gaussian, tinted warm red.
 * Shared by the live pipeline and [GlRenderer] so capture matches the preview. Programs and uniform
 * locations are created once in [init]; [run] allocates nothing. Needs a current GL context.
 */
internal class BloomPass {
    private var thresholdProgram = 0
    private var blurProgram = 0
    private var thresholdTexel = -1
    private var blurDir = -1
    private var blurGain = -1
    private val attachments = intArrayOf(GLES30.GL_COLOR_ATTACHMENT0)

    fun init() {
        thresholdProgram = buildProgram(VERTEX_SHADER, BLOOM_THRESHOLD_FRAGMENT_SHADER)
        blurProgram = buildProgram(VERTEX_SHADER, BLOOM_BLUR_FRAGMENT_SHADER)
        thresholdTexel = GLES30.glGetUniformLocation(thresholdProgram, "uTexel")
        blurDir = GLES30.glGetUniformLocation(blurProgram, "uDir")
        blurGain = GLES30.glGetUniformLocation(blurProgram, "uGain")
        GLES30.glUseProgram(thresholdProgram)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(thresholdProgram, "uSrc"), 0)
        GLES30.glUseProgram(blurProgram)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(blurProgram, "uSrc"), 0)
        if (BuildConfig.DEBUG) GlUtil.checkGl("bloom init")
    }

    /**
     * Runs downsample/threshold, horizontal blur and vertical blur+tint. [srcTexture] is the
     * adjusted frame ([srcWidth]x[srcHeight]). On return [BloomTargets.result] holds the bloom.
     * Leaves TEXTURE0 active with the framebuffer binding on a bloom target.
     */
    fun run(
        srcTexture: Int,
        srcWidth: Int,
        srcHeight: Int,
        halation: Float,
        targets: BloomTargets,
        quadVao: Int,
    ) {
        val w = targets.width
        val h = targets.height
        val scale = BloomMath.bloomScale(halation)
        GLES30.glViewport(0, 0, w, h)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)

        // B1: adjusted frame -> A (downsample + threshold).
        bind(targets.a)
        GLES30.glUseProgram(thresholdProgram)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTexture)
        GLES30.glUniform2f(thresholdTexel, 1f / srcWidth, 1f / srcHeight)
        draw(quadVao)

        // B2: A -> B, horizontal.
        bind(targets.b)
        GLES30.glUseProgram(blurProgram)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, targets.a.texture)
        GLES30.glUniform2f(blurDir, 1f / w, 0f)
        GLES30.glUniform3f(blurGain, 1f, 1f, 1f)
        draw(quadVao)

        // B3: B -> A, vertical, tinted and scaled by halation.
        bind(targets.a)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, targets.b.texture)
        GLES30.glUniform2f(blurDir, 0f, 1f / h)
        GLES30.glUniform3f(
            blurGain, BloomMath.TINT_R * scale, BloomMath.TINT_G * scale, BloomMath.TINT_B * scale,
        )
        draw(quadVao)
        // B is not needed any more.
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targets.b.framebuffer)
        GLES30.glInvalidateFramebuffer(GLES30.GL_FRAMEBUFFER, 1, attachments, 0)
        if (BuildConfig.DEBUG) GlUtil.checkGl("bloom pass")
    }

    /** Hints that the bloom result was consumed. Call after the composite has read it. */
    fun invalidateResult(targets: BloomTargets) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targets.a.framebuffer)
        GLES30.glInvalidateFramebuffer(GLES30.GL_FRAMEBUFFER, 1, attachments, 0)
    }

    fun release() {
        if (thresholdProgram != 0) GLES30.glDeleteProgram(thresholdProgram)
        if (blurProgram != 0) GLES30.glDeleteProgram(blurProgram)
        thresholdProgram = 0
        blurProgram = 0
    }

    private fun bind(target: RenderTarget) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
        // Every pixel is rewritten, so the old contents need not be loaded.
        GLES30.glInvalidateFramebuffer(GLES30.GL_FRAMEBUFFER, 1, attachments, 0)
    }

    private fun draw(quadVao: Int) {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }
}
