package com.example.photogether.core.render

import java.util.UUID

/** Kind of decoration placed on the photo. */
enum class OverlayKind { STICKER, TEXT }

/**
 * One sticker or text item. [x] and [y] are fractions of the output size (0..1), so the same
 * item lands in the same place on the preview and on the full-resolution export.
 * [scale] is relative to the output's shorter side.
 */
data class OverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: OverlayKind,
    val content: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

/** Effects and decorations painted on the photo. Ids are the labels used in the editor panels. */
data class OverlaySpec(
    val lightLeak: String? = null,
    val scratch: String? = null,
    val distortion: String? = null,
    /** Camera body skin painted over the photo, before the frame. [ViewfinderSkin.NONE] paints nothing. */
    val skin: ViewfinderSkin = ViewfinderSkin.NONE,
    val items: List<OverlayItem> = emptyList(),
)

/** Frame around the photo: a Polaroid frame, a theme or background image, and an optional caption. */
data class FrameSpec(
    val polaroid: PolaroidMode? = null,
    val themeId: String? = null,
    val backgroundUri: String? = null,
    val caption: String? = null,
    /** Polaroid style id from [PolaroidStyles]. Null or unknown ids give the classic look. */
    val polaroidStyleId: String = "classic",
    /** ARGB colour of the Polaroid border. */
    val borderColor: Int = 0xFFFFFFFF.toInt(),
    /** Pan and zoom of the photo inside a [PolaroidMode.FILL] frame. */
    val placement: PolaroidPlacement = PolaroidPlacement(),
    /** Poster frame (title above the photo). Null means no poster; [caption] is the title. */
    val poster: PosterTextMode? = null,
    /** Poster title size as a fraction of the full photo width, from [PosterTitle.MIN_SCALE] to 1. */
    val titleScale: Float = 1f,
    /** Horizontal alignment of the poster title inside the photo width. */
    val titleAlign: PosterTitleAlign = PosterTitleAlign.CENTER,
    /** Poster title font id from [PosterFonts]. Unknown ids give the default. */
    val titleFontId: String = PosterFonts.DEFAULT.id,
)

object PosterTitle {
    const val MIN_SCALE = 0.3f
}

/** Where the poster title sits across the photo width. */
enum class PosterTitleAlign { START, CENTER, END }

/** A font choice for the poster title. [family] and [style] go to Typeface.create. */
data class PosterFont(val id: String, val label: String, val family: String, val bold: Boolean, val italic: Boolean = false)

object PosterFonts {
    val DEFAULT = PosterFont("black", "Black", "sans-serif-black", bold = true)
    val ALL: List<PosterFont> = listOf(
        DEFAULT,
        PosterFont("condensed", "Condensed", "sans-serif-condensed", bold = true),
        PosterFont("serif", "Serif", "serif", bold = true),
        PosterFont("serif_italic", "Serif italic", "serif", bold = true, italic = true),
        PosterFont("thin", "Light", "sans-serif-light", bold = false),
        PosterFont("mono", "Mono", "monospace", bold = true),
        PosterFont("cursive", "Script", "cursive", bold = false),
    )

    fun byId(id: String?): PosterFont = ALL.firstOrNull { it.id == id } ?: DEFAULT
}

/**
 * Everything the editor can change. This is the unit that undo/redo stores.
 * Rendering order: GPU grade (filter + adjustments), crop, effects, frame, items.
 */
data class EditSnapshot(
    val params: EditParams = EditParams(),
    val overlays: OverlaySpec = OverlaySpec(),
    val frame: FrameSpec = FrameSpec(),
)
