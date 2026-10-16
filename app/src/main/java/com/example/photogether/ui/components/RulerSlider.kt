package com.example.photogether.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Spacing
import kotlin.math.floor
import kotlin.math.roundToInt

private val TICK_SPACING = 10.dp
private val RULER_HEIGHT = 40.dp
private const val MAJOR_TICK_EVERY = 5
private const val DISABLED_ALPHA = 0.4f

/**
 * Horizontal ruler for a continuous value. Drag to move the ruler under a fixed centre
 * indicator; one haptic tick per [step]. Double-tap resets to [defaultValue]. A dot after the
 * label marks a value that differs from the default.
 *
 * [ink] = false is the paper look used by the editor (thin ticks, cream centre line, paper colours).
 * [ink] = true is the camera look: a pill, a white knob at the centre, and ink colours.
 * [track] fills the ink pill (a gradient for white balance); null gives a dark translucent pill.
 * [showHeader] shows the label, the readout and the dot. Camera screens pass false, because the
 * value is shown elsewhere on the screen.
 */
@Composable
fun RulerSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    step: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueFormatter: (Float) -> String = { defaultRulerFormat(it, valueRange) },
    majorTickEvery: Int = MAJOR_TICK_EVERY,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    track: Brush? = null,
    showHeader: Boolean = true,
    ink: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val tickPx = with(LocalDensity.current) { TICK_SPACING.toPx() }
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val lastStep = remember { mutableIntStateOf(stepIndex(value, valueRange.start, step)) }

    val labelColor = if (ink) InkColors.textSecondary else AppColors.textSecondary
    val valueColor = if (ink) InkColors.textPrimary else AppColors.textPrimary
    val dotColor = if (ink) InkColors.highlight else AppColors.accent

    Column(
        modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else DISABLED_ALPHA),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
                    if (value != defaultValue) {
                        Spacer(Modifier.width(Spacing.xs))
                        Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                    }
                }
                Text(valueFormatter(value), style = MaterialTheme.typography.labelMedium, color = valueColor)
            }
            Spacer(Modifier.height(Spacing.xs))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(RULER_HEIGHT)
                .pointerInput(enabled, valueRange, step, tickPx) {
                    if (!enabled) return@pointerInput
                    var dragValue = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragValue = latestValue },
                        onDragEnd = { latestOnFinished() },
                        onDragCancel = { latestOnFinished() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // Dragging right moves the ruler right, so the value goes down.
                            dragValue = (dragValue - dragAmount / tickPx * step).coerceIn(valueRange)
                            val index = stepIndex(dragValue, valueRange.start, step)
                            if (index != lastStep.intValue) {
                                lastStep.intValue = index
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            latestOnChange(dragValue)
                        },
                    )
                }
                .pointerInput(enabled, defaultValue) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOnChange(defaultValue)
                            latestOnFinished()
                        },
                    )
                },
        ) {
            if (ink) {
                drawInkRuler(latestValue, valueRange, step, tickPx, majorTickEvery, track)
            } else {
                drawPaperRuler(latestValue, valueRange, step, tickPx, majorTickEvery)
            }
        }
    }
}

/** Paper ruler, unchanged from the editor's original look. */
private fun DrawScope.drawPaperRuler(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    tickPx: Float,
    majorTickEvery: Int,
) {
    val centre = size.width / 2f
    val fraction = (value - range.start) / step
    val lastIndex = ((range.endInclusive - range.start) / step).roundToInt()
    val visible = (centre / tickPx).toInt() + 2
    val base = floor(fraction).toInt()
    val minor = size.height * 0.35f
    val major = size.height * 0.6f
    val tickWidth = 1.dp.toPx()
    for (i in base - visible..base + visible) {
        if (i < 0 || i > lastIndex) continue
        val x = centre + (i - fraction) * tickPx
        val isMajor = i % majorTickEvery == 0
        val length = if (isMajor) major else minor
        drawLine(
            color = AppColors.textSecondary.copy(alpha = if (isMajor) 0.9f else 0.5f),
            start = Offset(x, size.height - length),
            end = Offset(x, size.height),
            strokeWidth = tickWidth,
        )
    }
    drawLine(
        color = AppColors.accent,
        start = Offset(centre, 0f),
        end = Offset(centre, size.height),
        strokeWidth = 2.dp.toPx(),
    )
}

/** Camera ruler: a pill, ticks centred vertically, and a white knob at the centre. */
private fun DrawScope.drawInkRuler(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    tickPx: Float,
    majorTickEvery: Int,
    track: Brush?,
) {
    val h = size.height
    val centre = size.width / 2f
    val fraction = (value - range.start) / step
    val lastIndex = ((range.endInclusive - range.start) / step).roundToInt()
    val visible = (centre / tickPx).toInt() + 2
    val base = floor(fraction).toInt()
    val minor = h * 0.3f
    val major = h * 0.5f
    val tickWidth = 1.dp.toPx()
    val knobRadius = h * 0.36f
    val corner = CornerRadius(h / 2f)

    // The pill. White balance gets its gradient; exposure gets a dark translucent pill.
    if (track != null) {
        drawRoundRect(brush = track, cornerRadius = corner)
    } else {
        drawRoundRect(color = InkColors.surfaceElevated.copy(alpha = 0.7f), cornerRadius = corner)
    }

    val tickColor = if (track != null) Color.Black else Color.White
    for (i in base - visible..base + visible) {
        if (i < 0 || i > lastIndex) continue
        val x = centre + (i - fraction) * tickPx
        if (x < 0f || x > size.width) continue
        val isMajor = i % majorTickEvery == 0
        val length = if (isMajor) major else minor
        drawLine(
            color = tickColor.copy(alpha = if (isMajor) 0.8f else 0.45f),
            start = Offset(x, (h - length) / 2f),
            end = Offset(x, (h + length) / 2f),
            strokeWidth = tickWidth,
        )
    }

    // Fixed knob at the centre: white, with a faint ring so it reads on the gradient.
    val knobCentre = Offset(centre, h / 2f)
    drawCircle(color = Color.White, radius = knobRadius, center = knobCentre)
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = knobRadius,
        center = knobCentre,
        style = Stroke(width = 1.dp.toPx()),
    )
}

private fun stepIndex(value: Float, start: Float, step: Float): Int =
    ((value - start) / step).roundToInt()

/** Default readout: the value as a whole percentage of the range, with a sign for bipolar ranges. */
private fun defaultRulerFormat(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val n = (value * 100f).roundToInt()
    return if (range.start < 0f && n > 0) "+$n" else "$n"
}
