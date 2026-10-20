package com.example.photogether.core.camera

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The stops a manual ISO and shutter wheel clicks through. Cameras expose a continuous sensor range;
 * the wheels use the familiar third-stop values inside that range.
 */
object ManualExposureSteps {
    private const val NS_PER_SECOND = 1_000_000_000L

    private val ISO_VALUES = listOf(
        50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800,
        1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000, 12800,
    )

    /** Shutter denominators from 1/8000 s to 1/3 s in third-stop steps. */
    private val SHUTTER_DENOMINATORS = listOf(
        8000, 6400, 5000, 4000, 3200, 2500, 2000, 1600, 1250, 1000, 800, 640, 500, 400, 320,
        250, 200, 160, 125, 100, 80, 60, 50, 40, 30, 25, 20, 15, 13, 10, 8, 6, 5, 4, 3,
    )

    /** Slow shutter stops above 1/3 s, as seconds in tenths. Capped at one second to keep the viewfinder usable. */
    private val SLOW_SHUTTER_TENTHS = listOf(4, 5, 6, 8, 10)

    private val SHUTTER_VALUES: List<Long> =
        SHUTTER_DENOMINATORS.map { NS_PER_SECOND / it } + SLOW_SHUTTER_TENTHS.map { it * NS_PER_SECOND / 10 }

    /** ISO stops inside [range], lowest first. Never empty: a range with no standard stop yields its lower end. */
    fun isoSteps(range: IntRange): List<Int> =
        ISO_VALUES.filter { it in range }.ifEmpty { listOf(range.first) }

    /** Shutter times in nanoseconds inside [rangeNs], fastest first. Never empty. */
    fun shutterSteps(rangeNs: LongRange): List<Long> =
        SHUTTER_VALUES.filter { it in rangeNs }.ifEmpty { listOf(rangeNs.first) }

    /** Index in [steps] of the value closest to [value]. */
    fun nearestIndex(steps: List<Number>, value: Number): Int {
        require(steps.isNotEmpty()) { "steps must not be empty" }
        val target = value.toDouble()
        return steps.indices.minBy { abs(steps[it].toDouble() - target) }
    }

    fun isoLabel(iso: Int): String = iso.toString()

    /** "1/250" for fast shutters, 0.5" or 1" from 0.4 s up. */
    fun shutterLabel(exposureNs: Long): String {
        val seconds = exposureNs.toDouble() / NS_PER_SECOND
        if (seconds >= 0.4) {
            val tenths = (seconds * 10).roundToInt()
            return if (tenths % 10 == 0) "${tenths / 10}\"" else "${tenths / 10}.${tenths % 10}\""
        }
        return "1/${(1.0 / seconds).roundToInt()}"
    }
}
