package com.example.photogether.core.render

/**
 * How the title of a poster frame is drawn.
 * [SOLID] prints it in the ink colour on the paper above the photo.
 * [CUTOUT] makes the letters a window: the photo continues up through them, so the title looks
 * sliced out of the picture.
 */
enum class PosterTextMode { SOLID, CUTOUT }

/**
 * Geometry of a poster frame ("masthead"). Everything is a fraction of the photo width, so the
 * frame scales with the photo. A header line and a hairline sit above the title, the title is
 * stretched across the photo width with its baseline on the photo top, and a footer strip sits
 * below the photo.
 */
data class PosterLayout(
    val frameWidth: Float,
    val frameHeight: Float,
    val photoDest: FloatRect,
    /** The title is fitted to [titleMaxWidth] and may not be taller than [titleMaxHeight]. */
    val titleMaxWidth: Float,
    val titleMaxHeight: Float,
    val cornerRadius: Float,
    val headerBaselineY: Float,
    val ruleY: Float,
    val footerBaselineY: Float,
    val infoSize: Float,
)

object PosterGeometry {
    const val SIDE_RATIO = 0.04f
    const val BOTTOM_RATIO = 0.10f
    const val TOP_RATIO = 0.42f
    const val TITLE_MAX_HEIGHT_RATIO = 0.30f

    /** The photo is shown whole, so the frame is as tall as the photo plus its top and bottom bands. */
    fun compute(photoWidth: Int, photoHeight: Int): PosterLayout {
        require(photoWidth > 0 && photoHeight > 0) { "Invalid size ${photoWidth}x$photoHeight" }
        val w = photoWidth.toFloat()
        val h = photoHeight.toFloat()
        val side = w * SIDE_RATIO
        val top = w * TOP_RATIO
        return PosterLayout(
            frameWidth = w + 2 * side,
            frameHeight = top + h + w * BOTTOM_RATIO,
            photoDest = FloatRect(side, top, side + w, top + h),
            titleMaxWidth = w,
            titleMaxHeight = w * TITLE_MAX_HEIGHT_RATIO,
            cornerRadius = w * 0.025f,
            headerBaselineY = w * 0.065f,
            ruleY = w * 0.085f,
            footerBaselineY = top + h + w * BOTTOM_RATIO * 0.62f,
            infoSize = w * 0.022f,
        )
    }

    /**
     * Source rectangle of a [srcW] x [srcH] photo for a window of [destW] x [destH] when the
     * photo must cover the window (centre crop). Used by the cutout, whose window is taller than
     * the photo because it reaches up through the letters.
     */
    fun coverSource(srcW: Int, srcH: Int, destW: Float, destH: Float): FloatRect {
        val sw = srcW.toFloat()
        val sh = srcH.toFloat()
        val scale = maxOf(destW / sw, destH / sh)
        val cw = destW / scale
        val ch = destH / scale
        val left = (sw - cw) / 2f
        val top = (sh - ch) / 2f
        return FloatRect(left, top, left + cw, top + ch)
    }
}
