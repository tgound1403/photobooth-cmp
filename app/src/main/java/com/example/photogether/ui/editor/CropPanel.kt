package com.example.photogether.ui.editor

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.photogether.core.render.CropAspect
import com.example.photogether.core.render.CropParams
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Spacing

/** Aspect presets, quarter-turn rotation and flips. Each tap is a discrete, undoable change. */
@Composable
fun CropPanel(
    crop: CropParams,
    onCrop: (CropParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SectionLabel("Aspect")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CropAspect.values().forEach { aspect ->
                StyleTile(
                    label = aspect.label(),
                    selected = crop.aspect == aspect,
                    onClick = { onCrop(crop.copy(aspect = aspect)) },
                ) { ink -> AspectGlyph(aspect, ink) }
            }
        }
        SectionLabel("Transform")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StyleTile(
                label = stringResource(R.string.crop_rotate),
                selected = false,
                onClick = { onCrop(crop.copy(quarterTurns = (crop.quarterTurns + 1) % 4)) },
            ) { ink -> RotateGlyph(crop.quarterTurns, ink) }
            StyleTile(
                label = stringResource(R.string.crop_flip_h),
                selected = crop.flipHorizontal,
                onClick = { onCrop(crop.copy(flipHorizontal = !crop.flipHorizontal)) },
            ) { ink -> FlipGlyph(horizontal = true, ink = ink) }
            StyleTile(
                label = stringResource(R.string.crop_flip_v),
                selected = crop.flipVertical,
                onClick = { onCrop(crop.copy(flipVertical = !crop.flipVertical)) },
            ) { ink -> FlipGlyph(horizontal = false, ink = ink) }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.textSecondary)
}

private fun CropAspect.label(): String =
    when (this) {
        CropAspect.FREE -> "Free"
        CropAspect.SQUARE -> "1:1"
        CropAspect.R4_5 -> "4:5"
        CropAspect.R3_4 -> "3:4"
        CropAspect.R9_16 -> "9:16"
    }

/** Frame drawn at the aspect's proportions. FREE is a dashed landscape frame. */
@Composable
private fun AspectGlyph(aspect: CropAspect, ink: Color) {
    val ratio = when (aspect) {
        CropAspect.FREE -> 4f / 3f
        CropAspect.SQUARE -> 1f
        CropAspect.R4_5 -> 4f / 5f
        CropAspect.R3_4 -> 3f / 4f
        CropAspect.R9_16 -> 9f / 16f
    }
    val dash = if (aspect == CropAspect.FREE) PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null
    Canvas(modifier = Modifier.size(36.dp)) {
        val box = size.minDimension
        val w = if (ratio >= 1f) box else box * ratio
        val h = if (ratio >= 1f) box / ratio else box
        drawRect(
            ink,
            Offset((size.width - w) / 2f, (size.height - h) / 2f),
            Size(w, h),
            style = DrawStroke(1.5f.dp.toPx(), pathEffect = dash),
        )
    }
}

/** Landscape frame turned by the current quarter turns, so the tile shows the rotation so far. */
@Composable
private fun RotateGlyph(quarterTurns: Int, ink: Color) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val w = size.minDimension * 0.7f
        val h = w * 0.6f
        rotate(degrees = 90f * quarterTurns, pivot = center) {
            drawRect(
                ink,
                Offset(center.x - w / 2f, center.y - h / 2f),
                Size(w, h),
                style = DrawStroke(1.5f.dp.toPx()),
            )
        }
    }
}

/** Two mirrored triangles split by an axis: filled is the original, outlined is its mirror image. */
@Composable
private fun FlipGlyph(horizontal: Boolean, ink: Color) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val s = size.minDimension * 0.8f
        val l = (size.width - s) / 2f
        val t = (size.height - s) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val gap = s * 0.06f
        val (original, mirror) = if (horizontal) {
            triangle(Offset(cx - gap, t), Offset(cx - gap, t + s), Offset(l, t + s)) to
                triangle(Offset(cx + gap, t), Offset(cx + gap, t + s), Offset(l + s, t + s))
        } else {
            triangle(Offset(l, cy - gap), Offset(l + s, cy - gap), Offset(l, t)) to
                triangle(Offset(l, cy + gap), Offset(l + s, cy + gap), Offset(l, t + s))
        }
        drawPath(original, ink)
        drawPath(mirror, ink, style = DrawStroke(1.dp.toPx()))
        if (horizontal) {
            drawLine(ink, Offset(cx, t), Offset(cx, t + s), strokeWidth = 1.dp.toPx())
        } else {
            drawLine(ink, Offset(l, cy), Offset(l + s, cy), strokeWidth = 1.dp.toPx())
        }
    }
}

private fun triangle(a: Offset, b: Offset, c: Offset): Path =
    Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        close()
    }
