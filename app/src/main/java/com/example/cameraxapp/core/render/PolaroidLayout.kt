package com.example.cameraxapp.core.render

import kotlin.math.max
import kotlin.math.min

/** Float rectangle in frame or source pixel coordinates, [right] and [bottom] exclusive. */
data class FloatRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** How a photo sits inside a Polaroid frame. */
enum class PolaroidMode {
    /** Whole photo visible, letterboxed inside a square window. */
    FIT,

    /** Photo cropped (centre square) to fill the window. */
    FILL,
}

/**
 * Geometry of a Polaroid frame for a source photo.
 * [photoArea] is the square window; [photoDest] is where the photo is drawn inside the frame
 * and [sourceRect] is the part of the source that is drawn there.
 * [captionBox] is the strip that holds the caption, top or bottom depending on the style.
 */
data class PolaroidLayout(
    val frameWidth: Float,
    val frameHeight: Float,
    val photoArea: FloatRect,
    val photoDest: FloatRect,
    val sourceRect: FloatRect,
    val captionBox: FloatRect = FloatRect(0f, 0f, 0f, 0f),
)

object PolaroidGeometry {
    /** Side margins and top margin, as a fraction of the square window side. */
    const val MARGIN_RATIO = 0.06f

    /** Bottom strip that holds the date, as a fraction of the square window side. */
    const val BOTTOM_RATIO = 0.26f

    /**
     * Layout for a [photoWidth] x [photoHeight] photo. The window side is the longer photo side,
     * so a portrait, landscape or square photo always fits without the caller having to guess.
     * [style] sets the margins and caption strip. [placement] pans and zooms the photo in
     * [PolaroidMode.FILL]; [PolaroidMode.FIT] ignores it.
     */
    fun compute(
        photoWidth: Int,
        photoHeight: Int,
        mode: PolaroidMode,
        style: PolaroidStyle = PolaroidStyles.CLASSIC,
        placement: PolaroidPlacement = PolaroidPlacement(),
    ): PolaroidLayout {
        require(photoWidth > 0 && photoHeight > 0) { "Invalid size ${photoWidth}x$photoHeight" }
        val w = photoWidth.toFloat()
        val h = photoHeight.toFloat()
        val side = max(w, h)
        val sideMargin = side * style.sideRatio
        val top = side * style.topRatio
        val bottom = side * style.bottomRatio
        val area = FloatRect(sideMargin, top, sideMargin + side, top + side)
        val frameWidth = side + 2 * sideMargin
        val frameHeight = top + side + bottom
        val captionBox =
            when (style.captionPlacement) {
                CaptionPlacement.BOTTOM -> FloatRect(0f, area.bottom, frameWidth, frameHeight)
                CaptionPlacement.TOP -> FloatRect(0f, 0f, frameWidth, area.top)
            }
        val full = FloatRect(0f, 0f, w, h)

        return when (mode) {
            PolaroidMode.FIT -> {
                val scale = min(side / w, side / h)
                val dw = w * scale
                val dh = h * scale
                val dest =
                    FloatRect(
                        left = area.left + (side - dw) / 2f,
                        top = area.top + (side - dh) / 2f,
                        right = area.left + (side - dw) / 2f + dw,
                        bottom = area.top + (side - dh) / 2f + dh,
                    )
                PolaroidLayout(frameWidth, frameHeight, area, dest, full, captionBox)
            }
            PolaroidMode.FILL -> {
                val p = placement.clamped()
                val crop = min(w, h) / p.zoom
                val cx = w / 2f + p.offsetX * (w - crop) / 2f
                val cy = h / 2f + p.offsetY * (h - crop) / 2f
                val source = FloatRect(cx - crop / 2f, cy - crop / 2f, cx + crop / 2f, cy + crop / 2f)
                PolaroidLayout(frameWidth, frameHeight, area, area, source, captionBox)
            }
        }
    }
}
