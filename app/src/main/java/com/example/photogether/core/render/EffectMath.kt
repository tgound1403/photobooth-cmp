package com.example.photogether.core.render

import kotlin.random.Random

/**
 * Pure maths for the per-pixel editor effects (chromatic aberration, light leak, distortion presets,
 * scratches). The present shader holds the same formulas; these functions give the uniform values
 * and are what the unit tests check. They reproduce the former CPU OverlayRenderer.
 */
object EffectMath {
    /** Horizontal channel shift in pixels at strength 1 (pixels, not a fraction, as the CPU pass did). */
    const val CHROMATIC_PX = 20f

    /** Largest chromatic strength: the editor slider (1) plus a distortion preset (1). */
    const val CHROMATIC_MAX = 2f

    /** Upper bound on scratch lines (20 + nextInt(30) gives 20..49). Matches the shader array size. */
    const val SCRATCH_MAX_LINES = 49

    /** Strength of the grain distortion preset, and the amplitude one unit of it adds (+-). */
    const val GRAIN_AMPLITUDE = 0.2f

    /** Shift of the red channel to the left and the blue channel to the right, in pixels. */
    fun chromaticShiftPx(strength: Float): Float = CHROMATIC_PX * strength.coerceIn(0f, CHROMATIC_MAX)

    /** One distortion preset: which effects it turns on and how strongly. */
    data class DistortionPreset(val chromatic: Float = 0f, val grain: Float = 0f, val vignette: Float = 0f)

    fun distortionPreset(type: String?): DistortionPreset =
        when (type) {
            "Distortion 1" -> DistortionPreset(chromatic = 0.3f)
            "Distortion 2" -> DistortionPreset(chromatic = 0.6f)
            "Distortion 3" -> DistortionPreset(grain = 0.5f)
            "Distortion 4" -> DistortionPreset(vignette = 0.7f)
            "Distortion 5" -> DistortionPreset(chromatic = 1.0f)
            else -> DistortionPreset()
        }

    /**
     * Light leak placement: the gradient's centre is a corner of the image (each component 0 or 1,
     * as a fraction of the size) and its radius is [radiusFactor] times the longer side.
     */
    data class LeakGradient(val cornerX: Float, val cornerY: Float, val radiusFactor: Float)

    fun leakGradient(type: String): LeakGradient {
        val hash = type.hashCode()
        return LeakGradient(
            cornerX = if (hash % 2 == 0) 0f else 1f,
            cornerY = if (hash % 3 == 0) 0f else 1f,
            radiusFactor = 0.5f + (hash % 5) / 10f,
        )
    }

    /** Straight (non-premultiplied) RGBA of the leak gradient at t = distance / radius, all in 0..1. */
    fun leakRgba(t: Float): FloatArray {
        val x = t.coerceAtLeast(0f)
        return when {
            x >= 1f -> floatArrayOf(0f, 0f, 0f, 0f)
            x < 0.5f -> {
                val u = x / 0.5f
                floatArrayOf(1f, lerp(200f, 100f, u) / 255f, 0f, lerp(150f, 100f, u) / 255f)
            }
            else -> {
                val u = (x - 0.5f) / 0.5f
                floatArrayOf(lerp(1f, 0f, u), lerp(100f, 0f, u) / 255f, 0f, lerp(100f, 0f, u) / 255f)
            }
        }
    }

    /** Opacity of the black vignette at t = distance from the centre / (0.9 * longer side). */
    fun vignetteAlpha(t: Float, intensity: Float): Float {
        val i = intensity.coerceIn(0f, 1f)
        if (i <= 0f) return 0f
        return ((t - (1f - i)) / i).coerceIn(0f, 1f) * i
    }

    /**
     * Scratch segments for [type] on a [width] x [height] image, as flat (x0, y0, x1, y1) pixel
     * values. The same label always gives the same lines (seeded by the label's hash).
     */
    fun scratchLines(type: String, width: Int, height: Int): FloatArray {
        val random = Random(type.hashCode())
        val count = 20 + random.nextInt(30)
        val out = FloatArray(count * 4)
        for (i in 0 until count) {
            val startX = random.nextFloat() * width
            val startY = random.nextFloat() * height
            out[i * 4] = startX
            out[i * 4 + 1] = startY
            out[i * 4 + 2] = startX + (random.nextFloat() - 0.5f) * 100
            out[i * 4 + 3] = startY + (random.nextFloat() - 0.5f) * 100
        }
        return out
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

/** The per-pixel effects of one edit, resolved from the editor state. [isNone] means no effect pass. */
data class EffectSpec(
    val chromatic: Float = 0f,
    val leak: String? = null,
    val grain: Float = 0f,
    val vignette: Float = 0f,
    val scratch: String? = null,
) {
    val isNone: Boolean
        get() = chromatic <= 0f && leak == null && grain <= 0f && vignette <= 0f && scratch == null

    companion object {
        val NONE = EffectSpec()

        /** [chromatic] is the slider; a distortion preset can add to it. */
        fun of(chromatic: Float, lightLeak: String?, distortion: String?, scratch: String?): EffectSpec {
            val d = EffectMath.distortionPreset(distortion)
            return EffectSpec(
                chromatic = (chromatic + d.chromatic).coerceIn(0f, EffectMath.CHROMATIC_MAX),
                leak = lightLeak,
                grain = d.grain,
                vignette = d.vignette,
                scratch = scratch,
            )
        }
    }
}
