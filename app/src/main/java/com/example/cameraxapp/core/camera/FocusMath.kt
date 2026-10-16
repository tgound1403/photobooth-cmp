package com.example.cameraxapp.core.camera

import com.example.cameraxapp.core.render.FloatRect

/**
 * Pure focus-point math for the camera preview. No Android types are used, so every function
 * can be unit tested on the JVM.
 */
object FocusMath {

    private val CENTRE = 0.5f to 0.5f

    /**
     * Maps a tap at ([x], [y]) inside a view of [w] x [h] pixels to normalized coordinates in
     * 0..1. Taps outside the view are clamped to its edge. Returns the centre when the view has no
     * positive size or any input is NaN.
     */
    fun normalize(x: Float, y: Float, w: Float, h: Float): Pair<Float, Float> {
        // Written as !(a > 0) so that NaN sizes also fall back to the centre.
        if (!(w > 0f) || !(h > 0f) || x.isNaN() || y.isNaN()) return CENTRE
        return (x / w).coerceIn(0f, 1f) to (y / h).coerceIn(0f, 1f)
    }

    /**
     * Square of side [size] centred on ([x], [y]), shifted so it lies fully inside [0, w] x [0, h].
     * An axis where [size] is larger than the frame is centred instead.
     */
    fun indicatorRect(x: Float, y: Float, size: Float, w: Float, h: Float): FloatRect {
        val left = placeAxis(x - size / 2f, size, w)
        val top = placeAxis(y - size / 2f, size, h)
        return FloatRect(left, top, left + size, top + size)
    }

    private fun placeAxis(start: Float, size: Float, extent: Float): Float =
        if (size > extent) {
            (extent - size) / 2f
        } else {
            start.coerceIn(0f, extent - size)
        }
}
