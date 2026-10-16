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
