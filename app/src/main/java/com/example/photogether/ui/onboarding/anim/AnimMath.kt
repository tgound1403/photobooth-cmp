package com.example.photogether.ui.onboarding.anim

import kotlin.math.abs
import kotlin.math.pow

/** Pure timeline helpers for the onboarding illustrations. All times are in 0..1 of one loop. */
object AnimMath {
    /** Progress of [t] through the window [from]..[to], clamped to 0..1. */
    fun segment(t: Float, from: Float, to: Float): Float =
        if (to <= from) (if (t >= to) 1f else 0f) else ((t - from) / (to - from)).coerceIn(0f, 1f)

    fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

    fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)

    fun easeInOut(x: Float): Float =
        if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f

    /** 0 outside the window, rising to 1 and falling back: a short pulse over [from]..[to]. */
    fun pulse(t: Float, from: Float, to: Float): Float {
        val s = segment(t, from, to)
        return 1f - abs(2f * s - 1f)
    }

    /** Fade in over [fadeIn] of the window, hold, fade out over the last [fadeOut]. */
    fun window(t: Float, from: Float, to: Float, fade: Float = 0.04f): Float =
        minOf(segment(t, from, from + fade), 1f - segment(t, to - fade, to))
}
