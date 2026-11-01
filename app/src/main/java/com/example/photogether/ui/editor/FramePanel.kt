package com.example.photogether.ui.editor

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.Spacing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.photogether.core.render.FrameSpec
import com.example.photogether.core.render.PolaroidMode
import com.example.photogether.core.render.PolaroidPlacement
import com.example.photogether.core.render.PosterTextMode
import com.example.photogether.core.render.PosterFonts
import com.example.photogether.core.render.PosterTitle
import com.example.photogether.core.render.PosterTitleAlign
import com.example.photogether.data.model.FrameThemes
import com.example.photogether.ui.components.RulerSlider
import com.example.photogether.ui.view.DeviceImagePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Frame choices: none, Polaroid (fit or fill), a theme, or a photo background.
 * While a Polaroid is selected, [PolaroidControls] sets its style, border colour and caption.
 * The date chip applies to theme and background frames only.
 *
 * [onFrameLive] receives changes that should update the preview without an undo step (typing a
 * caption). [onCommit] runs when that typing is finished. Both default so existing callers still
 * compile.
 */
@Composable
fun FramePanel(
    frame: FrameSpec,
    onFrame: (FrameSpec) -> Unit,
    modifier: Modifier = Modifier,
    onFrameLive: (FrameSpec) -> Unit = onFrame,
    onCommit: () -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FrameStyleTile(
                label = stringResource(R.string.option_none),
                glyph = FrameGlyph.NONE,
                selected = frame.polaroid == null && frame.poster == null && frame.themeId == null && frame.backgroundUri == null,
                onClick = { onFrame(FrameSpec()) },
            )
            PolaroidMode.values().forEach { mode ->
                FrameStyleTile(
                    label = if (mode == PolaroidMode.FIT) "Polaroid fit" else "Polaroid fill",
                    glyph = FrameGlyph.POLAROID,
                    selected = frame.polaroid == mode,
                    onClick = {
                        if (frame.polaroid != mode) {
                            // Switching between fit and fill keeps style, colour and caption.
                            // Pan and zoom are reset because they only apply to FILL.
                            onFrame(
                                if (frame.polaroid == null) {
                                    FrameSpec(polaroid = mode)
                                } else {
                                    frame.copy(polaroid = mode, placement = PolaroidPlacement())
                                },
                            )
                        }
                    },
                )
            }
            PosterTextMode.values().forEach { mode ->
                FrameStyleTile(
                    label = if (mode == PosterTextMode.SOLID) "Poster" else "Cutout",
                    glyph = if (mode == PosterTextMode.SOLID) FrameGlyph.POSTER else FrameGlyph.CUTOUT,
                    selected = frame.poster == mode,
                    onClick = {
                        if (frame.poster != mode) {
                            // Switching mode keeps the title and colour.
                            onFrame(
                                if (frame.poster == null) {
                                    FrameSpec(poster = mode, borderColor = POSTER_PAPER, caption = frame.caption)
                                } else {
                                    frame.copy(poster = mode)
                                },
                            )
                        }
                    },
                )
            }
            FrameStyleTile(
                label = stringResource(R.string.frame_photo),
                glyph = FrameGlyph.PHOTO,
                selected = frame.backgroundUri != null,
                onClick = { showPicker = true },
            )
        }

        if (frame.polaroid != null) {
            PolaroidControls(
                frame = frame,
                onFrame = onFrame,
                onFrameLive = onFrameLive,
                onCommit = onCommit,
            )
        }

        if (frame.poster != null) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CaptionField(
                    value = frame.caption.orEmpty(),
                    onValueChange = { text -> onFrameLive(frame.copy(caption = text.ifBlank { null })) },
                    onDone = onCommit,
                )
                BorderColorDots(
                    selected = frame.borderColor,
                    onSelect = { color -> onFrame(frame.copy(borderColor = color)) },
                )
                PosterTitleControls(frame = frame, onFrame = onFrame)
                RulerSlider(
                    label = "Title size",
                    value = frame.titleScale,
                    valueRange = PosterTitle.MIN_SCALE..1f,
                    defaultValue = 1f,
                    step = 0.05f,
                    onValueChange = { v -> onFrameLive(frame.copy(titleScale = v)) },
                    onValueChangeFinished = onCommit,
                )
            }
        }

        if (frame.polaroid == null && frame.poster == null) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                EditorChip(
                    label = stringResource(R.string.frame_date),
                    selected = frame.caption != null,
                    onClick = {
                        onFrame(frame.copy(caption = if (frame.caption == null) todayText() else null))
                    },
                )
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(FrameThemes.ALL_THEMES, key = { it.id }) { theme ->
                    EditorChip(
                        label = theme.name,
                        selected = frame.themeId == theme.id,
                        onClick = { onFrame(FrameSpec(themeId = theme.id, caption = frame.caption)) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        DeviceImagePicker(
            onImageSelected = { uri ->
                onFrame(FrameSpec(backgroundUri = uri.toString(), caption = frame.caption))
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** Alignment segment and font chips for the poster title. */
@Composable
private fun PosterTitleControls(frame: FrameSpec, onFrame: (FrameSpec) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PosterTitleAlign.values().forEach { align ->
            EditorChip(
                label = when (align) {
                    PosterTitleAlign.START -> "Left"
                    PosterTitleAlign.CENTER -> "Center"
                    PosterTitleAlign.END -> "Right"
                },
                selected = frame.titleAlign == align,
                onClick = { onFrame(frame.copy(titleAlign = align)) },
            )
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PosterFonts.ALL.forEach { font ->
            EditorChip(
                label = font.label,
                selected = frame.titleFontId == font.id,
                onClick = { onFrame(frame.copy(titleFontId = font.id)) },
            )
        }
    }
}

/** Cream paper colour of a new poster frame; one of the border palette colours. */
private const val POSTER_PAPER = 0xFFF6F0E1.toInt()

private fun todayText(): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

private enum class FrameGlyph { NONE, POLAROID, POSTER, CUTOUT, PHOTO }

/** Thumbnail tile for one frame style, drawn on the shared [StyleTile]. */
@Composable
private fun FrameStyleTile(
    label: String,
    glyph: FrameGlyph,
    selected: Boolean,
    onClick: () -> Unit,
) {
    StyleTile(label = label, selected = selected, onClick = onClick) { ink ->
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val pad = w * 0.14f
            val photo = Size(w - 2 * pad, h * 0.55f)
            when (glyph) {
                FrameGlyph.NONE -> drawRect(ink, Offset(pad, pad), Size(w - 2 * pad, h - 2 * pad), style = DrawStroke(1.dp.toPx()))
                FrameGlyph.POLAROID -> {
                    drawRect(ink, Offset(pad, pad), Size(w - 2 * pad, h - 2 * pad), style = DrawStroke(1.dp.toPx()))
                    drawRect(ink, Offset(pad, h - pad - h * 0.14f), Size(w - 2 * pad, h * 0.14f))
                }
                FrameGlyph.POSTER -> {
                    drawRect(ink, Offset(pad, pad), Size(w - 2 * pad, h * 0.12f))
                    drawRect(ink, Offset(pad, pad + h * 0.2f), photo, style = DrawStroke(1.dp.toPx()))
                }
                FrameGlyph.CUTOUT -> {
                    drawRect(ink, Offset(pad, pad), Size(w - 2 * pad, h * 0.22f))
                    drawRect(ink, Offset(pad, pad + h * 0.22f), Size(w - 2 * pad, h * 0.3f))
                }
                FrameGlyph.PHOTO -> drawRect(ink, Offset(pad, pad), Size(w - 2 * pad, h - 2 * pad), style = DrawStroke(1.dp.toPx()))
            }
        }
    }
}
