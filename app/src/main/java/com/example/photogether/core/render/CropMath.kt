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

    /**
     * The saved crop as seen in a view of [viewW] x [viewH] that shows a frame of [frameRatio] (width / height)
     * with FILL_CENTER (uniform scale to cover the view, centred). The crop is the centred [cropRatio] part of
     * the frame, clipped to the view. This is the one rule shared by the mask and the capture.
     */
    fun visibleCrop(viewW: Float, viewH: Float, frameRatio: Float, cropRatio: Float): FloatRect =
        clipTo(unclippedCrop(viewW, viewH, frameRatio, cropRatio), viewW, viewH)

    /** [rect] intersected with the view [viewW] x [viewH]. */
    fun clipTo(rect: FloatRect, viewW: Float, viewH: Float): FloatRect =
        FloatRect(maxOf(0f, rect.left), maxOf(0f, rect.top), minOf(viewW, rect.right), minOf(viewH, rect.bottom))

    /** Same as [visibleCrop] but not clipped to the view, so it can extend past the view edges. */
    fun unclippedCrop(viewW: Float, viewH: Float, frameRatio: Float, cropRatio: Float): FloatRect {
        require(viewW > 0f && viewH > 0f && frameRatio > 0f && cropRatio > 0f) { "Invalid geometry" }
        val scale = maxOf(viewW / frameRatio, viewH)
        val frameW = frameRatio * scale
        val frameH = scale
        val cropW: Float
        val cropH: Float
        if (frameW / frameH > cropRatio) { cropH = frameH; cropW = frameH * cropRatio } else { cropW = frameW; cropH = frameW / cropRatio }
        val cx = viewW / 2f
        val cy = viewH / 2f
        return FloatRect(cx - cropW / 2f, cy - cropH / 2f, cx + cropW / 2f, cy + cropH / 2f)
    }

    /** [rect] shrunk about its centre to 1 / [factor] of its size. A factor of 1 or less returns [rect]. */
    fun zoomCrop(rect: FloatRect, factor: Float): FloatRect {
        if (factor <= 1f) return rect
        val cx = (rect.left + rect.right) / 2f
        val cy = (rect.top + rect.bottom) / 2f
        val halfW = rect.width / factor / 2f
        val halfH = rect.height / factor / 2f
        return FloatRect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }

    /** Centred part of [rect] that keeps 1 / [factor] of each side, at least 1 px. A factor of 1 or less returns [rect]. */
    fun zoomCrop(rect: PixelRect, factor: Float): PixelRect {
        if (factor <= 1f) return rect
        val w = (rect.width / factor).roundToInt().coerceIn(1, rect.width)
        val h = (rect.height / factor).roundToInt().coerceIn(1, rect.height)
        val left = rect.left + (rect.width - w) / 2
        val top = rect.top + (rect.height - h) / 2
        return PixelRect(left, top, left + w, top + h)
    }
}
