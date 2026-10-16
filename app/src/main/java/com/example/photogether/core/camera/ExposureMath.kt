package com.example.photogether.core.camera

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Discrete exposure-compensation range. Positions are integer indices; each index is worth
 * [step] EV, so the camera's own step size (usually 1/3 or 1/6 EV) maps directly onto a slider.
 */
data class EvRange(
    val minIndex: Int,
    val maxIndex: Int,
    val step: Float,
) {
    init {
        require(maxIndex >= minIndex) { "maxIndex must be >= minIndex" }
        require(step > 0f) { "step must be positive" }
    }

    val minEv: Float get() = minIndex * step

    val maxEv: Float get() = maxIndex * step

    /** Index closest to [ev], clamped into [minIndex, maxIndex]. */
    fun indexFor(ev: Float): Int = (ev / step).roundToInt().coerceIn(minIndex, maxIndex)

    /** EV value of [index], clamped into the range. */
    fun evFor(index: Int): Float = index.coerceIn(minIndex, maxIndex) * step

    /** Number of steps per full stop; used to place a longer tick every stop. */
    val majorTickEvery: Int get() = (1f / step).roundToInt().coerceAtLeast(1)

    companion object {
        /** Used when the camera offers no hardware EV: +-2 EV in 1/3 stop steps, matching EditParams.softwareEv. */
        val SOFTWARE = EvRange(minIndex = -6, maxIndex = 6, step = 1f / 3f)
    }
}

/**
 * Formats an EV value with one decimal and an explicit sign for non-zero values,
 * e.g. "+0.7", "0.0", "-1.3". Values that round to zero are shown as "0.0" (never "-0.0").
 */
fun formatEv(ev: Float): String {
    val tenths = (ev * 10f).roundToInt()
    if (tenths == 0) return "0.0"
    val sign = if (tenths > 0) "+" else "-"
    val magnitude = abs(tenths)
    return "$sign${magnitude / 10}.${magnitude % 10}"
}
