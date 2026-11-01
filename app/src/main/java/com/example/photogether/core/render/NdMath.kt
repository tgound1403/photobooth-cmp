package com.example.photogether.core.render

import kotlin.math.pow

/**
 * Pure maths for the ND and graduated-ND filters. The adjust shader gets the stops as uniforms and
 * applies [gain] in linear light; 0 stops means no change, so shaders that never set them are unaffected.
 */
object NdMath {
    /** Darkening at slider 1 for the whole-frame ND (a real ND8 is 3 stops). */
    const val MAX_ND_STOPS = 3f

    /** Darkening at the very top of the frame at slider 1 for the graduated ND. */
    const val MAX_GND_STOPS = 2.5f

    /** The graduation starts fading at this fraction of the height from the top and is gone by [GRAD_END]. */
    const val GRAD_START = 0.15f
    const val GRAD_END = 0.65f

    fun ndStops(nd: Float): Float = nd.coerceIn(0f, 1f) * MAX_ND_STOPS

    fun gndStops(gnd: Float): Float = gnd.coerceIn(0f, 1f) * MAX_GND_STOPS

    /** Linear-light multiplier for [stops] of density. */
    fun gain(stops: Float): Float = 2f.pow(-stops)

    /** Stops of graduated density at [fromTop] (0 = top edge, 1 = bottom edge). Smoothstep, like the shader. */
    fun gradStopsAt(gnd: Float, fromTop: Float): Float {
        val t = ((fromTop - GRAD_START) / (GRAD_END - GRAD_START)).coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)
        return gndStops(gnd) * (1f - smooth)
    }
}
