package com.example.photogether.core.render

import kotlin.math.min

/** Where the caption strip sits on a Polaroid frame. */
enum class CaptionPlacement { BOTTOM, TOP }

/**
 * One Polaroid frame design. Margins and caption size are fractions of the square photo window
 * side, so every style scales with the photo.
 */
data class PolaroidStyle(
    val id: String,
    val label: String,
    val sideRatio: Float,
    val topRatio: Float,
    val bottomRatio: Float,
    val captionSizeRatio: Float,
    val captionPlacement: CaptionPlacement,
    val fontFamily: String,
    val bold: Boolean,
)

object PolaroidStyles {
    /** Original look: thin side borders, date in the bottom strip. */
    val CLASSIC = PolaroidStyle(
        id = "classic",
        label = "C1",
        sideRatio = 0.06f,
        topRatio = 0.06f,
        bottomRatio = 0.26f,
        captionSizeRatio = 0.35f,
        captionPlacement = CaptionPlacement.BOTTOM,
        fontFamily = "sans-serif-bold",
        bold = false,
    )

    /** Tall bottom strip with large caption text. */
    val BIG_TEXT = PolaroidStyle(
        id = "big_text",
        label = "B1",
        sideRatio = 0.06f,
        topRatio = 0.06f,
        bottomRatio = 0.40f,
        captionSizeRatio = 0.45f,
        captionPlacement = CaptionPlacement.BOTTOM,
        fontFamily = "sans-serif-black",
        bold = true,
    )

    /** Very thin border all round, small bottom strip. */
    val THIN = PolaroidStyle(
        id = "thin",
        label = "S1",
        sideRatio = 0.03f,
        topRatio = 0.03f,
        bottomRatio = 0.12f,
        captionSizeRatio = 0.45f,
        captionPlacement = CaptionPlacement.BOTTOM,
        fontFamily = "sans-serif-black",
        bold = true,
    )

    /** Wide top strip for the caption, thin bottom edge. */
    val TITLE_TOP = PolaroidStyle(
        id = "title_top",
        label = "T1",
        sideRatio = 0.06f,
        topRatio = 0.22f,
        bottomRatio = 0.08f,
        captionSizeRatio = 0.45f,
        captionPlacement = CaptionPlacement.TOP,
        fontFamily = "sans-serif-black",
        bold = true,
    )

    /** All styles, in the order the picker shows them. */
    val ALL: List<PolaroidStyle> = listOf(CLASSIC, BIG_TEXT, THIN, TITLE_TOP)

    /** Style for [id]. Null or unknown ids give [CLASSIC]. */
    fun byId(id: String?): PolaroidStyle = ALL.firstOrNull { it.id == id } ?: CLASSIC
}

object FrameColors {
    /** Border colours offered in the panel, in display order. ARGB ints. */
    val PALETTE: List<Int> = listOf(
        0xFFFFFFFF.toInt(), // white
        0xFFF6F0E1.toInt(), // cream
        0xFFE8DCC4.toInt(), // beige
        0xFFDCE8F2.toInt(), // light blue
        0xFF5B6B4A.toInt(), // moss green
        0xFF3A3A3C.toInt(), // dark grey
        0xFF111111.toInt(), // black
    )

    /** Black text on light borders, white text on dark ones, by perceived brightness. */
    fun textColorOn(bg: Int): Int {
        val r = (bg shr 16) and 0xFF
        val g = (bg shr 8) and 0xFF
        val b = bg and 0xFF
        val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        return if (luma > 0.5f) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}

/**
 * Pan and zoom of the photo inside a FILL Polaroid window. [zoom] runs from 1 (centred square
 * crop) to 4. [offsetX] and [offsetY] run from -1 (one edge) to 1 (the other edge), 0 is centred.
 */
data class PolaroidPlacement(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /** Same placement with zoom and offsets pulled into range. NaN resets that value to its default. */
    fun clamped(): PolaroidPlacement =
        PolaroidPlacement(
            zoom = if (zoom.isNaN()) 1f else zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
            offsetX = if (offsetX.isNaN()) 0f else offsetX.coerceIn(-1f, 1f),
            offsetY = if (offsetY.isNaN()) 0f else offsetY.coerceIn(-1f, 1f),
        )

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
    }
}

/** Turns drag and pinch input into a [PolaroidPlacement]. Pure maths, no Android types. */
object PolaroidGesture {

    /**
     * Side in pixels of the square photo window when the frame is drawn in a [boxW] x [boxH] box.
     * It depends on the style only, not on the photo, so the same drag pans the same amount for
     * any photo.
     */
    fun windowSidePx(boxW: Float, boxH: Float, style: PolaroidStyle): Float =
        min(boxW / (1f + 2f * style.sideRatio), boxH / (style.topRatio + 1f + style.bottomRatio))

    /**
     * Placement after a drag of ([panPxX], [panPxY]) pixels and a pinch of [zoomBy] on a
     * [photoW] x [photoH] photo. [windowSidePx] is the size from [windowSidePx]. An axis with no
     * slack (square crop at zoom 1) keeps its offset.
     */
    fun apply(
        p: PolaroidPlacement,
        panPxX: Float,
        panPxY: Float,
        zoomBy: Float,
        windowSidePx: Float,
        photoW: Int,
        photoH: Int,
    ): PolaroidPlacement {
        val base = p.clamped()
        if (photoW <= 0 || photoH <= 0) return base

        val zoom = (base.zoom * finiteOr(zoomBy, 1f)).coerceIn(PolaroidPlacement.MIN_ZOOM, PolaroidPlacement.MAX_ZOOM)
        val w = photoW.toFloat()
        val h = photoH.toFloat()
        val crop = min(w, h) / zoom
        val slackX = (w - crop) / 2f
        val slackY = (h - crop) / 2f
        val panX = finiteOr(panPxX, 0f)
        val panY = finiteOr(panPxY, 0f)

        var offsetX = base.offsetX
        var offsetY = base.offsetY
        if (slackX > 0f && windowSidePx > 0f) offsetX -= (panX / windowSidePx) * crop / slackX
        if (slackY > 0f && windowSidePx > 0f) offsetY -= (panY / windowSidePx) * crop / slackY

        return PolaroidPlacement(zoom, offsetX, offsetY).clamped()
    }

    private fun finiteOr(value: Float, fallback: Float): Float = if (value.isFinite()) value else fallback
}
