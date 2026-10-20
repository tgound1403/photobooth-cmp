package com.example.photogether.core.camera

import kotlin.math.roundToInt

/**
 * Angle in degrees of the point ([x], [y]) around ([cx], [cy]). 0 is to the right and the angle grows
 * clockwise on screen, because screen y points down.
 */
fun angleDegrees(x: Float, y: Float, cx: Float, cy: Float): Float =
    Math.toDegrees(kotlin.math.atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()

/** Signed shortest turn from [from] to [to], in (-180, 180]. Crossing the 180 degree seam stays a small step. */
fun shortestTurn(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d > 180f) d -= 360f else if (d <= -180f) d += 360f
    return d
}

/** Wheel angle of detent [index]. The first detent sits at 0 degrees and the angle grows with the index. */
fun detentAngle(index: Int, perDetent: Float): Float = index * perDetent

/** Detent nearest to [angle] on a wheel of [count] detents [perDetent] degrees apart, clamped to the wheel's ends. */
fun detentIndexFor(angle: Float, perDetent: Float, count: Int): Int =
    (angle / perDetent).roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))
