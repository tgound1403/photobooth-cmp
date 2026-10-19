package com.example.photogether.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.photogether.core.camera.closestOption
import com.example.photogether.core.camera.ratioLabel
import com.example.photogether.core.render.CameraAspect
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import kotlin.math.roundToInt
import com.example.photogether.ui.theme.AppIcons

/**
 * Dark button under the viewfinder that shows the current aspect ratio, such as "4:3", inside corner brackets.
 * Tapping it opens the aspect panel.
 */
@Composable
fun AspectButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Tỷ lệ khung hình $label" }
            .size(width = 72.dp, height = 44.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val inset = 8.dp.toPx()
            val arm = 9.dp.toPx()
            val stroke = 1.5.dp.toPx()
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val tint = InkColors.textPrimary
            drawLine(tint, Offset(left, top), Offset(left + arm, top), stroke)
            drawLine(tint, Offset(left, top), Offset(left, top + arm), stroke)
            drawLine(tint, Offset(right, top), Offset(right - arm, top), stroke)
            drawLine(tint, Offset(right, top), Offset(right, top + arm), stroke)
            drawLine(tint, Offset(left, bottom), Offset(left + arm, bottom), stroke)
            drawLine(tint, Offset(left, bottom), Offset(left, bottom - arm), stroke)
            drawLine(tint, Offset(right, bottom), Offset(right - arm, bottom), stroke)
            drawLine(tint, Offset(right, bottom), Offset(right, bottom - arm), stroke)
        }
        Text(
            text = label,
            style = BinsooType.label,
            color = InkColors.textPrimary,
        )
    }
}

/**
 * Aspect panel under the viewfinder: a scrolling row of ratio chips, and a dark chevron-down button below them that
 * closes the panel. The chip for [selected] is cream.
 */
@Composable
fun AspectPanel(
    selected: CameraAspect,
    onSelect: (CameraAspect) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Five equal chips fill the width, so nothing is clipped or scrolls.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CameraAspect.entries.forEach { aspect ->
                AspectChip(
                    aspect = aspect,
                    selected = aspect == selected,
                    onClick = { onSelect(aspect) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CameraCircleButton(
            icon = AppIcons.ChevronDown,
            label = "Đóng tỷ lệ khung hình",
            onClick = onClose,
            buttonSize = 40.dp,
        )
    }
}

/** One ratio chip: rounded square, dark, cream when selected. */
@Composable
private fun AspectChip(
    aspect: CameraAspect,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Tỷ lệ ${aspect.label}" }
            .height(56.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(toolFill(selected))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = aspect.label,
            style = BinsooType.label,
            color = toolContent(selected),
        )
    }
}

/**
 * Lens panel under the viewfinder: a dark capsule with one option per focal length in [options], each showing the
 * 35mm-equivalent number and its magnification against [mainMm] underneath. The option closest to [currentMm] is a
 * cream circle. Tapping an option calls [onSelect] with its focal length.
 */
@Composable
fun LensPanel(
    options: List<Float>,
    currentMm: Float,
    mainMm: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = closestOption(options, currentMm)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
    ) {
        options.forEach { mm ->
            val isSelected = mm == selected
            Column(
                modifier = Modifier
                    .semantics { contentDescription = "Tiêu cự ${mm.roundToInt()}mm" }
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable { onSelect(mm) }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) InkColors.accent else Color.Transparent)
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${mm.roundToInt()}",
                        style = BinsooType.title,
                        color = if (isSelected) InkColors.onAccent else InkColors.textPrimary,
                    )
                }
                Text(
                    text = ratioLabel(mm, mainMm),
                    style = BinsooType.label,
                    color = InkColors.textSecondary,
                )
            }
        }
    }
}
