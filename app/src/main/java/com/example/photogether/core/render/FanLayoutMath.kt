package com.example.photogether.core.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** A point in screen coordinates (x right, y down). */
data class FanPoint(val x: Float, val y: Float)

/**
 * Pure layout math for the filter fan: entries on an arc around a centre. Angles are in degrees in screen
 * coordinates (y down), so 270 is straight up, 200 is left and slightly up, 340 is right and slightly up.
 */
object FanLayoutMath {
    const val START_DEG = 200f
    const val END_DEG = 340f

    /** More entries than this are split across two concentric arcs. */
    const val MAX_PER_ARC = 9

    /** Position of entry [index] of [count] on the arc of [radius] around ([cx], [cy]). One entry sits at the arc's middle. */
    fun entryPosition(
        index: Int,
        count: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        startDeg: Float = START_DEG,
        endDeg: Float = END_DEG,
    ): FanPoint {
        val t = if (count <= 1) 0.5f else index.toFloat() / (count - 1)
        val rad = (startDeg + (endDeg - startDeg) * t) * PI.toFloat() / 180f
        return FanPoint(cx + radius * cos(rad), cy + radius * sin(rad))
    }

    /**
     * Positions for [count] entries. Up to [MAX_PER_ARC] fit on one arc of [outerRadius]; more are split, the
     * first half on the inner arc ([ringGap] closer to the centre) and the rest on the outer one. Index order
     * runs inner arc first, each arc from left to right.
     */
    fun layout(
        count: Int,
        cx: Float,
        cy: Float,
        outerRadius: Float,
        ringGap: Float,
    ): List<FanPoint> {
        if (count <= 0) return emptyList()
        if (count <= MAX_PER_ARC) {
            return List(count) { entryPosition(it, count, cx, cy, outerRadius) }
        }
        val inner = count / 2
        val outer = count - inner
        val innerRadius = (outerRadius - ringGap).coerceAtLeast(0f)
        return List(inner) { entryPosition(it, inner, cx, cy, innerRadius) } +
            List(outer) { entryPosition(it, outer, cx, cy, outerRadius) }
    }

    /** Index of the point nearest ([x], [y]) if it lies within [hitRadius] of it, else null. */
    fun nearestIndex(points: List<FanPoint>, x: Float, y: Float, hitRadius: Float): Int? {
        var best: Int? = null
        var bestDist = Float.MAX_VALUE
        points.forEachIndexed { i, p ->
            val d = hypot(p.x - x, p.y - y)
            if (d <= hitRadius && d < bestDist) {
                best = i
                bestDist = d
            }
        }
        return best
    }

    // region Placement inside the window

    /** Chip bounding box in screen coordinates. */
    data class FanBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val centerX: Float get() = (left + right) / 2f
    }

    /** Where the fan sits: every chip centre, the chip box, and the arc that was chosen. */
    data class FanPlacement(
        val points: List<FanPoint>,
        val box: FanBox,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val midDeg: Float,
    )

    /** Fan opens upward, to the left, downward or to the right of the button. Listed in tie-break order after the preferred one. */
    private val DIRECTIONS = listOf(270f, 180f, 90f, 0f)

    /**
     * Places the fan so that every chip lies inside a [width] x [height] window with [margin] on each side
     * and [topReserve] extra room above (for the name label). The fan opens in the direction that needs the
     * smallest shift of its centre from the ([anchorX], [anchorY]) button, trying [preferredMidDeg] first on a tie. If even
     * the best direction does not fit, the radius shrinks.
     *
     * Entries are laid on [spanDeg] wide arcs; a ring takes as many chips as fit at [spacing] apart, and
     * extra rings sit [ringGap] closer to the centre. Index order runs from the inner ring outward.
     */
    fun place(
        count: Int,
        anchorX: Float,
        anchorY: Float,
        radius: Float,
        ringGap: Float,
        chipSize: Float,
        spacing: Float,
        width: Float,
        height: Float,
        margin: Float,
        topReserve: Float,
        preferredMidDeg: Float = 270f,
        spanDeg: Float = 120f,
    ): FanPlacement {
        val half = chipSize / 2f
        val order = listOf(preferredMidDeg) + DIRECTIONS.filter { it != preferredMidDeg }
        var best: FanPlacement? = null
        var bestCost = Float.MAX_VALUE
        for (mid in order) {
            val start = mid - spanDeg / 2f
            val end = mid + spanDeg / 2f
            val ext = unitExtents(start, end)
            val availW = width - 2f * margin - chipSize
            val availH = height - 2f * margin - topReserve - chipSize
            val spanX = (ext.maxX - ext.minX).coerceAtLeast(0.01f)
            val spanY = (ext.maxY - ext.minY).coerceAtLeast(0.01f)
            val r = min(radius, min(availW / spanX, availH / spanY)).coerceAtLeast(MIN_RADIUS)
            val cx = anchorX.coerceInRange(margin + half - ext.minX * r, width - margin - half - ext.maxX * r)
            val cy = anchorY.coerceInRange(margin + topReserve + half - ext.minY * r, height - margin - half - ext.maxY * r)
            val cost = hypot(cx - anchorX, cy - anchorY) + (radius - r) * 2f
            if (cost < bestCost - 0.5f) {
                bestCost = cost
                best = build(count, cx, cy, r, ringGap, spacing, start, end, mid, half, ext)
            }
        }
        return best ?: build(count, anchorX, anchorY, radius, ringGap, spacing, 200f, 340f, 270f, half, unitExtents(200f, 340f))
    }

    private const val MIN_RADIUS = 60f
    private const val MIN_RING_RADIUS = 48f
    private const val MAX_RINGS = 4

    private class Extents(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    /** Bounding box of an arc of radius 1 centred on the origin, including any axis extremes the arc passes. */
    private fun unitExtents(startDeg: Float, endDeg: Float): Extents {
        val angles = mutableListOf(startDeg, endDeg)
        var a = ceil(startDeg / 90f) * 90f
        while (a <= endDeg) {
            angles += a
            a += 90f
        }
        val xs = angles.map { cos(it * PI.toFloat() / 180f) }
        val ys = angles.map { sin(it * PI.toFloat() / 180f) }
        return Extents(xs.min(), xs.max(), ys.min(), ys.max())
    }

    private fun build(
        count: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        ringGap: Float,
        spacing: Float,
        startDeg: Float,
        endDeg: Float,
        mid: Float,
        half: Float,
        ext: Extents,
    ): FanPlacement {
        val spanRad = (endDeg - startDeg) * PI.toFloat() / 180f
        fun capacity(r: Float) = floor(spanRad * r / spacing).toInt() + 1
        // Smallest number of rings (outer one first) that can hold every entry.
        var rings = 1
        while (rings < MAX_RINGS && (0 until rings).sumOf { capacity(radius - it * ringGap) } < count &&
            radius - rings * ringGap >= MIN_RING_RADIUS
        ) rings++
        // Inner ring first, each taking an even share of what is left but never more than it can hold.
        val points = ArrayList<FanPoint>(count)
        var left = count
        for (k in rings - 1 downTo 0) {
            val r = (radius - k * ringGap).coerceAtLeast(MIN_RING_RADIUS)
            val ringsLeft = k + 1
            val take = if (k == 0) left else min(capacity(r), ceil(left.toFloat() / ringsLeft).toInt())
            repeat(take) { points += entryPosition(it, take, cx, cy, r, startDeg, endDeg) }
            left -= take
        }
        val box = FanBox(
            left = cx + ext.minX * radius - half,
            top = cy + ext.minY * radius - half,
            right = cx + ext.maxX * radius + half,
            bottom = cy + ext.maxY * radius + half,
        )
        return FanPlacement(points, box, cx, cy, radius, mid)
    }

    /** Like coerceIn, but returns the low bound when the range is empty (the window is too small). */
    private fun Float.coerceInRange(lo: Float, hi: Float): Float = if (hi < lo) lo else coerceIn(lo, hi)

    /**
     * Two to three letters standing for [name] on a fan chip: the initials of the first words when there are
     * several, otherwise the first three letters. Upper case. Empty names give an empty string.
     */
    fun shortCode(name: String): String {
        val words = name.trim().split(Regex("[\\s\\-_]+")).filter { it.isNotEmpty() }
        val letters = words.map { w -> w.first() }
        return when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(3).uppercase()
            else -> letters.take(3).joinToString("").uppercase()
        }
    }

    // endregion
}
