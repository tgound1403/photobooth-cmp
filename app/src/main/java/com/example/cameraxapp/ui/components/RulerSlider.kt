package com.example.cameraxapp.ui.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.Spacing
import kotlin.math.floor
import kotlin.math.roundToInt

private val TICK_SPACING = 10.dp
private val RULER_HEIGHT = 40.dp
private const val MAJOR_TICK_EVERY = 5

/**
 * Horizontal ruler for a continuous value. Drag to move the ruler under a fixed centre
 * indicator; one haptic tick per [step]. Double-tap resets to [defaultValue]. A dot after the
 * label marks a value that differs from the default.
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
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val tickPx = with(LocalDensity.current) { TICK_SPACING.toPx() }
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val lastStep = remember { mutableIntStateOf(stepIndex(value, valueRange.start, step)) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.textSecondary)
                if (value != defaultValue) {
                    Spacer(Modifier.width(Spacing.xs))
                    Box(Modifier.size(6.dp).background(AppColors.accent, CircleShape))
                }
            }
            Text(formatValue(value, valueRange), style = MaterialTheme.typography.labelMedium, color = AppColors.textPrimary)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(RULER_HEIGHT)
                .pointerInput(valueRange, step, tickPx) {
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
                .pointerInput(defaultValue) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOnChange(defaultValue)
                            latestOnFinished()
                        },
                    )
                },
        ) {
            drawRuler(latestValue, valueRange, step, tickPx)
        }
    }
}

private fun DrawScope.drawRuler(value: Float, range: ClosedFloatingPointRange<Float>, step: Float, tickPx: Float) {
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
        val isMajor = i % MAJOR_TICK_EVERY == 0
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

private fun stepIndex(value: Float, start: Float, step: Float): Int =
    ((value - start) / step).roundToInt()

/** Shows the value as a whole percentage of the range, with a sign for bipolar ranges. */
private fun formatValue(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val n = (value * 100f).roundToInt()
    return if (range.start < 0f && n > 0) "+$n" else "$n"
}
