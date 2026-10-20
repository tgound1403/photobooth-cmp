package com.example.photogether.core.camera

import com.example.photogether.core.render.snapKelvin
import kotlin.math.exp
import kotlin.math.ln

/** Blue-to-red gain ratio an AWB typically settles on under 5500 K daylight. Sensors differ, so this is approximate. */
private const val DAYLIGHT_BLUE_RED_RATIO = 0.85f

/** Slope of ln(K / 5500) against ln(blue gain / red gain), fitted to tungsten (3000 K) and shade (7500 K). */
private const val RATIO_SLOPE = 0.88f

private const val NEUTRAL_K = 5500f

/**
 * Rough colour temperature of the scene, from the white balance gains the camera's auto white balance
 * chose. A warm scene makes the camera boost blue, so a high blue-to-red gain ratio means a low Kelvin.
 * Returns null when a gain is not positive. The result is snapped to the Kelvin step.
 */
fun estimateKelvin(redGain: Float, blueGain: Float): Int? {
    if (redGain <= 0f || blueGain <= 0f) return null
    val logRatio = ln(blueGain / redGain / DAYLIGHT_BLUE_RED_RATIO)
    return snapKelvin(NEUTRAL_K * exp(-RATIO_SLOPE * logRatio))
}
