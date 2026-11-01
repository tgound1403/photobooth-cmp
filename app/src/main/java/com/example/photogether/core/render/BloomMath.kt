package com.example.photogether.core.render

import kotlin.math.exp

/**
 * Pure maths for the halation/bloom pass. The bloom shaders in Shaders.kt are generated from these
 * numbers, so the shader and the tests agree.
 */
object BloomMath {
    /** Luminance where the highlight knee starts. */
    const val KNEE_START = 0.7f

    /** Luminance where the knee reaches full weight. */
    const val KNEE_END = 1.0f

    /** Bloom buffers are this many times smaller than the adjusted frame in each dimension. */
    const val DOWNSCALE = 4

    /** Number of blur taps per direction. */
    const val TAPS = 9

    /** Gaussian sigma in bloom-buffer texels. */
    const val SIGMA = 2.0f

    /** Warm-red halation tint (multiplied by [bloomScale]). */
    const val TINT_R = 1.0f
    const val TINT_G = 0.45f
    const val TINT_B = 0.35f

    /** 0 below [KNEE_START], 1 at [KNEE_END], smoothstep in between. Mirrors the shader's smoothstep. */
    fun thresholdWeight(luma: Float): Float {
        val t = ((luma - KNEE_START) / (KNEE_END - KNEE_START)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Normalised symmetric Gaussian weights, index taps / 2 is the centre. */
    fun gaussianWeights(taps: Int = TAPS): FloatArray {
        require(taps >= 1 && taps % 2 == 1) { "taps must be odd and positive, was $taps" }
        val half = taps / 2
        val raw = DoubleArray(taps) { i ->
            val x = (i - half).toDouble()
            exp(-(x * x) / (2.0 * SIGMA * SIGMA))
        }
        val sum = raw.sum()
        return FloatArray(taps) { (raw[it] / sum).toFloat() }
    }

    /** Halation strength in [0, 1]; 0 (or less) means the bloom pass is skipped. */
    fun bloomScale(halation: Float): Float = if (halation <= 0f) 0f else halation.coerceAtMost(1f)

    /** Size of a bloom buffer for a frame dimension. */
    fun bloomSize(full: Int): Int = maxOf(1, full / DOWNSCALE)

    /**
     * One blurred highlight layer of the editor's bloom pass. [kneeStart]..[kneeEnd] is the smoothstep that
     * picks what glows, [spread] widens the blur (1 = the live pass), and [gainR]/[gainG]/[gainB] tint and
     * scale the result. The layers are added together before the screen composite.
     */
    data class Layer(
        val kneeStart: Float,
        val kneeEnd: Float,
        val spread: Float,
        val gainR: Float,
        val gainG: Float,
        val gainB: Float,
    )

    /** Strongest total gain a single layer reaches at slider 1. */
    private const val BLOOM_GAIN = 0.85f
    private const val GLOW_GAIN = 0.60f
    private const val DREAMY_GAIN = 0.45f

    /** Layers for the editor params; empty means no bloom pass at all. Halation keeps the live pass numbers. */
    fun layersFor(p: EditParams): List<Layer> = buildList {
        val h = bloomScale(p.halation)
        if (h > 0f) add(Layer(KNEE_START, KNEE_END, 1f, TINT_R * h, TINT_G * h, TINT_B * h))
        val b = p.bloom.coerceIn(0f, 1f)
        if (b > 0f) add(Layer(0.55f, 1.0f, 1.6f, BLOOM_GAIN * b, BLOOM_GAIN * b, BLOOM_GAIN * b))
        val g = p.glow.coerceIn(0f, 1f)
        if (g > 0f) add(Layer(0.30f, 0.90f, 2.6f, GLOW_GAIN * g, GLOW_GAIN * 0.92f * g, GLOW_GAIN * 0.78f * g))
        val d = p.dreamy.coerceIn(0f, 1f)
        if (d > 0f) add(Layer(0.0f, 0.35f, 3.2f, DREAMY_GAIN * d, DREAMY_GAIN * 0.96f * d, DREAMY_GAIN * d))
    }
}
