package com.example.photogether.core.render

import android.opengl.GLES30
import android.os.SystemClock
import android.util.Log
import com.tgound.photogether.BuildConfig
import kotlin.math.pow

/** Uniform locations of the adjust program, looked up once after link. */
internal class AdjustLocations(program: Int) {
    val texMatrix = GLES30.glGetUniformLocation(program, "uTexMatrix")
    val src = GLES30.glGetUniformLocation(program, "uSrc")
    val lutA = GLES30.glGetUniformLocation(program, "uLutA")
    val lutB = GLES30.glGetUniformLocation(program, "uLutB")
    val useLutA = GLES30.glGetUniformLocation(program, "uUseLutA")
    val useLutB = GLES30.glGetUniformLocation(program, "uUseLutB")
    val lutMix = GLES30.glGetUniformLocation(program, "uLutMix")
    val preGain = GLES30.glGetUniformLocation(program, "uPreGain")
    val filterIntensity = GLES30.glGetUniformLocation(program, "uFilterIntensity")
    val exposureGain = GLES30.glGetUniformLocation(program, "uExposureGain")
    val wbGain = GLES30.glGetUniformLocation(program, "uWbGain")
    val contrastFactor = GLES30.glGetUniformLocation(program, "uContrastFactor")
    val saturationFactor = GLES30.glGetUniformLocation(program, "uSaturationFactor")
    val toneAmount = GLES30.glGetUniformLocation(program, "uToneAmount")
    val highlight = GLES30.glGetUniformLocation(program, "uHighlight")
    val shadow = GLES30.glGetUniformLocation(program, "uShadow")
    val fadeAmount = GLES30.glGetUniformLocation(program, "uFadeAmount")
    val grainAmplitude = GLES30.glGetUniformLocation(program, "uGrainAmplitude")
    val vignetteStrength = GLES30.glGetUniformLocation(program, "uVignetteStrength")
    val grainSeed = GLES30.glGetUniformLocation(program, "uGrainSeed")
    val grainScale = GLES30.glGetUniformLocation(program, "uGrainScale")
}

/** Uniform locations of the sharpen/present program, looked up once after link. */
internal class SharpenLocations(program: Int) {
    val texMatrix = GLES30.glGetUniformLocation(program, "uTexMatrix")
    val src = GLES30.glGetUniformLocation(program, "uSrc")
    val texel = GLES30.glGetUniformLocation(program, "uTexel")
    val sharpness = GLES30.glGetUniformLocation(program, "uSharpness")
    val bloom = GLES30.glGetUniformLocation(program, "uBloom")
    val useBloom = GLES30.glGetUniformLocation(program, "uUseBloom")
    val leakTex = GLES30.glGetUniformLocation(program, "uLeakTex")
    val dustTex = GLES30.glGetUniformLocation(program, "uDustTex")
    val leak = GLES30.glGetUniformLocation(program, "uLeak")
    val dust = GLES30.glGetUniformLocation(program, "uDust")
    val leakOffset = GLES30.glGetUniformLocation(program, "uLeakOffset")
    val leakFlip = GLES30.glGetUniformLocation(program, "uLeakFlip")
    val dustOffset = GLES30.glGetUniformLocation(program, "uDustOffset")
    val useLeak = GLES30.glGetUniformLocation(program, "uUseLeak")
    val useDust = GLES30.glGetUniformLocation(program, "uUseDust")
    val useChromatic = GLES30.glGetUniformLocation(program, "uUseChromatic")
    val chromaticPx = GLES30.glGetUniformLocation(program, "uChromaticPx")
    val useLeakGrad = GLES30.glGetUniformLocation(program, "uUseLeakGrad")
    val leakGradCorner = GLES30.glGetUniformLocation(program, "uLeakGradCorner")
    val leakGradRadius = GLES30.glGetUniformLocation(program, "uLeakGradRadius")
    val useFxGrain = GLES30.glGetUniformLocation(program, "uUseFxGrain")
    val fxGrain = GLES30.glGetUniformLocation(program, "uFxGrain")
    val useFxVignette = GLES30.glGetUniformLocation(program, "uUseFxVignette")
    val fxVignette = GLES30.glGetUniformLocation(program, "uFxVignette")
    val scratchCount = GLES30.glGetUniformLocation(program, "uScratchCount")
    val scratchLines = GLES30.glGetUniformLocation(program, "uScratchLines")
    val stampStyle = GLES30.glGetUniformLocation(program, "uStampStyle")
    val stampDigits = GLES30.glGetUniformLocation(program, "uStampDigits")
    val stampScale = GLES30.glGetUniformLocation(program, "uStampScale")
    val stampOut = GLES30.glGetUniformLocation(program, "uStampOut")
    val stampImage = GLES30.glGetUniformLocation(program, "uStampImage")
    val stampRot = GLES30.glGetUniformLocation(program, "uStampRot")
    val stampTopDown = GLES30.glGetUniformLocation(program, "uStampTopDown")
}

/**
 * The adjust-pass uniform values derived from [EditParams], computed once per edit (not per frame)
 * with the same math as [GlUtil.applyAdjustUniforms].
 */
internal class AdjustValues(p: EditParams) {
    val preR: Float
    val preG: Float
    val preB: Float
    val wbR: Float
    val wbG: Float
    val wbB: Float
    val filterIntensity = p.filterIntensity
    val exposureGain = exposureGain(p.exposure)
    val contrastFactor = contrastFactor(p.contrast)
    val saturationFactor = saturationFactor(p.saturation)
    val highlight = p.highlight
    val shadow = p.shadow
    val fadeAmount = fadeAmount(p.fade)
    val grainAmplitude = grainAmplitude(p.grain)
    val vignetteStrength = vignetteStrength(p.vignette)

    init {
        val (wr, wg, wb) = whiteBalanceGains(p.temperature, p.tint)
        val (kr, kg, kb) = kelvinGains(p.kelvin)
        val ev = 2f.pow(p.softwareEv / 2.2f)
        preR = kr * ev
        preG = kg * ev
        preB = kb * ev
        wbR = wr
        wbG = wg
        wbB = wb
    }

    /** Allocation-free. The program must be in use. */
    fun apply(l: AdjustLocations) {
        GLES30.glUniform3f(l.preGain, preR, preG, preB)
        GLES30.glUniform1f(l.filterIntensity, filterIntensity)
        GLES30.glUniform1f(l.exposureGain, exposureGain)
        GLES30.glUniform3f(l.wbGain, wbR, wbG, wbB)
        GLES30.glUniform1f(l.contrastFactor, contrastFactor)
        GLES30.glUniform1f(l.saturationFactor, saturationFactor)
        GLES30.glUniform1f(l.toneAmount, TONE_AMPLITUDE)
        GLES30.glUniform1f(l.highlight, highlight)
        GLES30.glUniform1f(l.shadow, shadow)
        GLES30.glUniform1f(l.fadeAmount, fadeAmount)
        GLES30.glUniform1f(l.grainAmplitude, grainAmplitude)
        GLES30.glUniform1f(l.vignetteStrength, vignetteStrength)
    }
}

/**
 * Debug-only frame timing, logged under tag "GpuTime" every [LOG_EVERY] frames as avg/max ms.
 *
 * Android's SDK has no Java binding for EXT_disjoint_timer_query (GL_TIME_ELAPSED_EXT), so the GPU
 * figure is measured with fence syncs instead: a fence is inserted after each frame's commands and
 * polled without blocking on later frames. "gpu" is the time from submit to fence signal (an upper
 * bound that includes queueing, resolution limited to the frame cadence). "cpu" is the time spent
 * inside onFrameAvailable. Does nothing in release builds.
 */
internal class FrameTimer {
    private val enabled = BuildConfig.DEBUG
    private val fences = LongArray(RING)
    private val submitNs = LongArray(RING)
    private var writeIdx = 0
    private var readIdx = 0
    private var inFlight = 0
    private var cpuStartNs = 0L
    private var cpuCount = 0
    private var cpuSum = 0.0
    private var cpuMax = 0.0
    private var gpuCount = 0
    private var gpuSum = 0.0
    private var gpuMax = 0.0

    fun begin() {
        if (!enabled) return
        cpuStartNs = SystemClock.elapsedRealtimeNanos()
    }

    /** Call after the last GL command of the frame, with the context current. */
    fun end() {
        if (!enabled) return
        val now = SystemClock.elapsedRealtimeNanos()
        val cpuMs = (now - cpuStartNs) / 1_000_000.0
        cpuCount++
        cpuSum += cpuMs
        if (cpuMs > cpuMax) cpuMax = cpuMs

        if (inFlight < RING) {
            fences[writeIdx] = GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            submitNs[writeIdx] = cpuStartNs
            writeIdx = (writeIdx + 1) % RING
            inFlight++
        }
        while (inFlight > 0) {
            val status = GLES30.glClientWaitSync(fences[readIdx], 0, 0L)
            if (status != GLES30.GL_ALREADY_SIGNALED && status != GLES30.GL_CONDITION_SATISFIED) break
            val ms = (SystemClock.elapsedRealtimeNanos() - submitNs[readIdx]) / 1_000_000.0
            GLES30.glDeleteSync(fences[readIdx])
            fences[readIdx] = 0L
            readIdx = (readIdx + 1) % RING
            inFlight--
            gpuCount++
            gpuSum += ms
            if (ms > gpuMax) gpuMax = ms
        }
        if (cpuCount >= LOG_EVERY) {
            val gpuAvg = if (gpuCount > 0) gpuSum / gpuCount else 0.0
            Log.d(
                TAG,
                "frames=$cpuCount cpu avg=${"%.2f".format(cpuSum / cpuCount)}ms max=${"%.2f".format(cpuMax)}ms | " +
                    "gpu(fence) avg=${"%.2f".format(gpuAvg)}ms max=${"%.2f".format(gpuMax)}ms n=$gpuCount",
            )
            cpuCount = 0; cpuSum = 0.0; cpuMax = 0.0
            gpuCount = 0; gpuSum = 0.0; gpuMax = 0.0
        }
    }

    /** Call with the context current. */
    fun release() {
        for (i in 0 until RING) {
            if (fences[i] != 0L) GLES30.glDeleteSync(fences[i])
            fences[i] = 0L
        }
        writeIdx = 0
        readIdx = 0
        inFlight = 0
    }

    private companion object {
        const val TAG = "GpuTime"
        const val RING = 4
        const val LOG_EVERY = 120
    }
}

/**
 * Overlay (light leak + dust) uniform values derived from [EditParams] and a seed, computed once per
 * edit. The strengths are already scaled, as the present shader expects.
 */
internal class FxValues(p: EditParams, seed: Int) {
    val leak = FxMath.leakScale(p.leak)
    val dust = FxMath.dustScale(p.dust)
    val leakOffset = FxMath.leakOffset(seed)
    val dustOffset = FxMath.dustOffset(seed)
    val leakFlip = FxMath.leakFlip(seed)

    /**
     * Sets the overlay uniforms of the present program (must be in use) and binds the textures on
     * [FxTextures.LEAK_UNIT] / [FxTextures.DUST_UNIT]. An overlay whose texture failed to load is off.
     * Leaves texture unit 0 active. Allocation-free.
     */
    fun apply(l: SharpenLocations, tex: FxTextures) {
        val useLeak = leak > 0f && tex.leak != 0
        val useDust = dust > 0f && tex.dust != 0
        GLES30.glUniform1i(l.useLeak, if (useLeak) 1 else 0)
        GLES30.glUniform1i(l.useDust, if (useDust) 1 else 0)
        if (useLeak) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + FxTextures.LEAK_UNIT)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.leak)
            GLES30.glUniform1f(l.leak, leak)
            GLES30.glUniform2f(l.leakOffset, leakOffset.first, leakOffset.second)
            GLES30.glUniform1i(l.leakFlip, if (leakFlip) 1 else 0)
        }
        if (useDust) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + FxTextures.DUST_UNIT)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex.dust)
            GLES30.glUniform1f(l.dust, dust)
            GLES30.glUniform2f(l.dustOffset, dustOffset.first, dustOffset.second)
        }
        if (useLeak || useDust) GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }
}

/**
 * Editor-effect uniform values (chromatic aberration, light leak gradient, grain and vignette
 * presets, scratches) for an image of [width] x [height] pixels. Computed once per render.
 * The live preview never sets these; the flags default to 0 there, so the effect code is skipped.
 */
internal class EffectValues(private val spec: EffectSpec, width: Int, height: Int) {
    private val chromaticPx = EffectMath.chromaticShiftPx(spec.chromatic)
    private val leak = spec.leak?.let { EffectMath.leakGradient(it) }
    private val scratch = spec.scratch?.let { EffectMath.scratchLines(it, width, height) }

    /** Sets the uniforms of the present program, which must be in use. */
    fun apply(l: SharpenLocations) {
        GLES30.glUniform1i(l.useChromatic, if (chromaticPx > 0f) 1 else 0)
        if (chromaticPx > 0f) GLES30.glUniform1f(l.chromaticPx, chromaticPx)
        GLES30.glUniform1i(l.useLeakGrad, if (leak != null) 1 else 0)
        if (leak != null) {
            GLES30.glUniform2f(l.leakGradCorner, leak.cornerX, leak.cornerY)
            GLES30.glUniform1f(l.leakGradRadius, leak.radiusFactor)
        }
        GLES30.glUniform1i(l.useFxGrain, if (spec.grain > 0f) 1 else 0)
        if (spec.grain > 0f) GLES30.glUniform1f(l.fxGrain, spec.grain)
        GLES30.glUniform1i(l.useFxVignette, if (spec.vignette > 0f) 1 else 0)
        if (spec.vignette > 0f) GLES30.glUniform1f(l.fxVignette, spec.vignette)
        val lines = scratch
        val count = if (lines != null) lines.size / 4 else 0
        GLES30.glUniform1i(l.scratchCount, count)
        if (lines != null) GLES30.glUniform4fv(l.scratchLines, count, lines, 0)
    }
}

/**
 * Date stamp uniform values. Created once per edit; [apply] runs per draw and allocates nothing:
 * the six digits live in one reused array that is refreshed at most once a minute.
 * [fixedMillis] > 0 prints that instant (capture); otherwise the time passed to [apply] (live).
 */
internal class StampValues(
    style: DateStampStyle,
    private val fixedMillis: Long,
    private val quarterTurns: Int = 0, // clockwise turns that show the output upright (EXIF); 0 for live
) {
    private val styleCode = when (style) {
        DateStampStyle.NONE -> 0
        DateStampStyle.ORANGE_LCD -> 1
        DateStampStyle.WHITE_DIGITAL -> 2
    }
    val active: Boolean get() = styleCode != 0
    private val digits = IntArray(DateStampMath.DIGIT_COUNT)
    private var bucket = Long.MIN_VALUE

    /**
     * Sets the stamp uniforms of the present program (must be in use). [rowsFromTop] is true when
     * gl_FragCoord.y counts from the top of the image (offline read-back), false for a window surface.
     */
    fun apply(l: SharpenLocations, nowMs: Long, outW: Int, outH: Int, rowsFromTop: Boolean) {
        GLES30.glUniform1i(l.stampStyle, styleCode)
        if (styleCode == 0) return
        val millis = if (fixedMillis > 0L) fixedMillis else nowMs
        val b = millis / 60_000L
        if (b != bucket) {
            bucket = b
            DateStampMath.fillDigits(millis, java.util.TimeZone.getDefault(), digits)
        }
        GLES30.glUniform1iv(l.stampDigits, DateStampMath.DIGIT_COUNT, digits, 0)
        val turns = ((quarterTurns % 4) + 4) % 4
        val dispW = if (turns % 2 == 1) outH else outW
        val dispH = if (turns % 2 == 1) outW else outH
        GLES30.glUniform1f(l.stampScale, DateStampMath.stampScale(dispW, dispH))
        GLES30.glUniform2f(l.stampOut, dispW.toFloat(), dispH.toFloat())
        GLES30.glUniform2f(l.stampImage, outW.toFloat(), outH.toFloat())
        GLES30.glUniform1i(l.stampRot, turns)
        GLES30.glUniform1i(l.stampTopDown, if (rowsFromTop) 1 else 0)
    }
}
