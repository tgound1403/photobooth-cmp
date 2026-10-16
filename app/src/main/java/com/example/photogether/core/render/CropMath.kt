package com.example.photogether.core.render

import kotlin.math.roundToInt

/** Integer pixel rectangle, [right] and [bottom] exclusive. */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Pure crop geometry, shared by the bitmap renderer and JVM unit tests. */
object CropMath {

    /** Size of a width x height image after [quarterTurns] clockwise quarter turns. */
    fun rotatedSize(width: Int, height: Int, quarterTurns: Int): Pair<Int, Int> {
        val turns = ((quarterTurns % 4) + 4) % 4
        return if (turns % 2 == 1) height to width else width to height
    }

    /**
     * Largest centred rectangle inside a [width] x [height] image with the aspect ratio of [aspect].
     * [CropAspect.FREE] returns the whole image.
     */
    fun cropRect(width: Int, height: Int, aspect: CropAspect): PixelRect {
        require(width > 0 && height > 0) { "Invalid size ${width}x$height" }
        val target = aspect.ratio ?: return PixelRect(0, 0, width, height)
        return centerCrop(width, height, target)
    }

    /** Largest centred rectangle with width / height equal to [target], inside [width] x [height]. */
    fun centerCrop(width: Int, height: Int, target: Float): PixelRect {
        require(width > 0 && height > 0) { "Invalid size ${width}x$height" }
        val current = width.toFloat() / height
        return if (current > target) {
            // Image is wider than the target: keep full height, trim the sides.
            val w = (height * target).roundToInt().coerceIn(1, width)
            val left = (width - w) / 2
            PixelRect(left, 0, left + w, height)
        } else {
            // Image is taller than the target: keep full width, trim top and bottom.
            val h = (width / target).roundToInt().coerceIn(1, height)
            val top = (height - h) / 2
            PixelRect(0, top, width, top + h)
        }
    }
}
