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
)

/**
 * Everything the editor can change. This is the unit that undo/redo stores.
 * Rendering order: GPU grade (filter + adjustments), crop, effects, frame, items.
 */
data class EditSnapshot(
    val params: EditParams = EditParams(),
    val overlays: OverlaySpec = OverlaySpec(),
    val frame: FrameSpec = FrameSpec(),
)
