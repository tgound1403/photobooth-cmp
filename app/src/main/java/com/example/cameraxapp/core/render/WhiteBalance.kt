package com.example.cameraxapp.core.render

import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Kelvin white balance maths. Pure Kotlin, no Android dependencies, so it runs in JVM tests.
 *
 * Pipeline: Kelvin -> CIE xy (Kim et al. 2002 Planckian locus) -> linear sRGB,
 * then per-channel gain relative to 5500 K, gamma 2.2, clamp, and luma normalisation.
 */

object Kelvin {
    const val MIN = 2000
    const val MAX = 10000
    const val NEUTRAL = 5500
    const val SNAP = 50
}

/** Valid domain of the Kim et al. approximation. */
private const val WB_PLANCK_MIN_K = 1667.0
private const val WB_PLANCK_MAX_K = 25000.0

/** Floor applied to linear RGB before dividing, so the blue channel at low K stays finite. */
private const val WB_LINEAR_FLOOR = 1e-4

/** Gain channel bounds applied before luma normalisation. */
private const val WB_GAIN_MIN = 0.5f
private const val WB_GAIN_MAX = 2.0f

private const val WB_GAMMA = 2.2

/** Mired = 1e6 / K. The slider is linear in mired: 2000 K at pos 0, 10000 K at pos 1. */
private const val WB_MIRED_NUMERATOR = 1_000_000f
private const val WB_MIRED_WARM = 500f  // 2000 K
private const val WB_MIRED_COOL = 100f  // 10000 K

fun kelvinToMired(k: Float): Float = WB_MIRED_NUMERATOR / k

/** Slider position 0..1 to a snapped Kelvin value in [Kelvin.MIN, Kelvin.MAX]. */
fun sliderToKelvin(pos: Float): Int {
    val p = pos.coerceIn(0f, 1f)
    val mired = WB_MIRED_WARM - (WB_MIRED_WARM - WB_MIRED_COOL) * p
    return snapKelvin(WB_MIRED_NUMERATOR / mired)
}

/** Kelvin to slider position 0..1. Inverse of [sliderToKelvin] up to one snap step. */
fun kelvinToSlider(k: Int): Float {
    val mired = kelvinToMired(k.toFloat())
    return ((WB_MIRED_WARM - mired) / (WB_MIRED_WARM - WB_MIRED_COOL)).coerceIn(0f, 1f)
}

/** Clamp to [Kelvin.MIN, Kelvin.MAX] and round to the nearest multiple of [Kelvin.SNAP]. */
fun snapKelvin(k: Float): Int {
    val clamped = k.coerceIn(Kelvin.MIN.toFloat(), Kelvin.MAX.toFloat())
    return ((clamped / Kelvin.SNAP).roundToInt() * Kelvin.SNAP).coerceIn(Kelvin.MIN, Kelvin.MAX)
}

/** Chromaticity (x, y) of the Planckian locus at temperature [k]. */
fun planckianXy(k: Float): Pair<Float, Float> {
    val (x, y) = planckianXyD(k.toDouble())
    return x.toFloat() to y.toFloat()
}

/** Linear sRGB of a blackbody at [k], normalised to Y = 1, each channel floored at 1e-4. */
fun blackbodyLinearRgb(k: Float): Triple<Float, Float, Float> {
    val (x, y) = planckianXyD(k.toDouble())
    val (r, g, b) = xyToLinearSrgbD(x, y)
    return Triple(
        maxOf(r, WB_LINEAR_FLOOR).toFloat(),
        maxOf(g, WB_LINEAR_FLOOR).toFloat(),
        maxOf(b, WB_LINEAR_FLOOR).toFloat(),
    )
}

/**
 * Per-channel RGB gains for a Kelvin white balance, with luma normalised to 1.
 * Higher Kelvin gives a warmer image (Lightroom convention). Null means auto, so no correction.
 */
fun kelvinGains(kelvin: Int?): Triple<Float, Float, Float> {
    if (kelvin == null) return Triple(1f, 1f, 1f)
    val k = kelvin.coerceIn(Kelvin.MIN, Kelvin.MAX).toFloat()
    val ref = blackbodyLinearRgb(Kelvin.NEUTRAL.toFloat())
    val target = blackbodyLinearRgb(k)

    val r = wbChannelGain(ref.first, target.first)
    val g = wbChannelGain(ref.second, target.second)
    val b = wbChannelGain(ref.third, target.third)

    val lum = luma(r, g, b)
    return Triple(r / lum, g / lum, b / lum)
}

private fun wbChannelGain(reference: Float, target: Float): Float {
    val linear = reference / target
    val encoded = linear.toDouble().pow(1.0 / WB_GAMMA).toFloat()
    return encoded.coerceIn(WB_GAIN_MIN, WB_GAIN_MAX)
}

/** Kim et al. 2002 cubic approximation, three branches for y and two for x. */
private fun planckianXyD(k: Double): Pair<Double, Double> {
    val t = k.coerceIn(WB_PLANCK_MIN_K, WB_PLANCK_MAX_K)
    val t2 = t * t
    val t3 = t2 * t

    val x = if (t <= 4000.0) {
        -0.2661239e9 / t3 - 0.2343589e6 / t2 + 0.8776956e3 / t + 0.179910
    } else {
        -3.0258469e9 / t3 + 2.1070379e6 / t2 + 0.2226347e3 / t + 0.240390
    }

    val x2 = x * x
    val x3 = x2 * x
    val y = when {
        t <= 2222.0 -> -1.1063814 * x3 - 1.34811020 * x2 + 2.18555832 * x - 0.20219683
        t <= 4000.0 -> -0.9549476 * x3 - 1.37418593 * x2 + 2.09137015 * x - 0.16748867
        else -> 3.0817580 * x3 - 5.87338670 * x2 + 3.75112997 * x - 0.37001483
    }
    return x to y
}

/** XYZ with Y = 1, then sRGB linear via the standard D65 matrix. */
private fun xyToLinearSrgbD(x: Double, y: Double): Triple<Double, Double, Double> {
    val bigX = x / y
    val bigY = 1.0
    val bigZ = (1.0 - x - y) / y
    val r = 3.2406 * bigX - 1.5372 * bigY - 0.4986 * bigZ
    val g = -0.9689 * bigX + 1.8758 * bigY + 0.0415 * bigZ
    val b = 0.0557 * bigX - 0.2040 * bigY + 1.0570 * bigZ
    return Triple(r, g, b)
}
