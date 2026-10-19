package com.example.photogether.ui.components

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val TICK_SPACING = 10.dp
private val RULER_HEIGHT = 40.dp
private val SCALE_HEIGHT = 76.dp
private val SCALE_UNIT = 56.dp
private val SCALE_LABEL_SIZE = 15.sp
private val SCALE_RED = Color(0xFFE53935)
/** Scale ticks per full stop (1 EV): a tick every 1/6 EV, with numbers on the whole stops. */
private const val GRID_PER_UNIT = 6
private val SCALE_GRADIENT = listOf(Color(0xFF1C1C1C), Color(0xFF0A0A0A), Color(0xFF141414))
// Gradients depend only on colours, not on size, so they are built once instead of on every draw.
private val SCALE_BODY_BRUSH = Brush.verticalGradient(SCALE_GRADIENT)
private val SCALE_RIM_BRUSH = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)))
private val INK_SHEEN_BRUSH = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.28f)))
private val INK_RIM_BRUSH = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.04f)))
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
 * [scale] = true (with [ink]) is the camera scale: a black pill with numbered major ticks that scroll
 * under a red centre line, so the labels show how far the value has moved. One unit of value is
 * [SCALE_UNIT] wide.
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
    scale: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val tickPx = with(density) { TICK_SPACING.toPx() }
    val scaleUnitPx = with(density) { SCALE_UNIT.toPx() }
    val scaleLabelPx = with(density) { SCALE_LABEL_SIZE.toPx() }
    // Scale mode moves the ruler by value, so one step is a fixed fraction of a unit on screen.
    val pxPerStep = if (scale) scaleUnitPx * step else tickPx
    val latestPx by rememberUpdatedState(pxPerStep)
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val lastStep = remember { mutableIntStateOf(stepIndex(value, valueRange.start, step)) }
    // Width of the ink pill, so a drag can turn finger distance into value distance.
    val widthState = remember { mutableFloatStateOf(0f) }
    val knobEdgePx = with(density) { RULER_HEIGHT.toPx() * 0.36f }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            // A soft drop shadow lifts the numbers off the gradient.
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.argb(170, 0, 0, 0))
        }
    }

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
                .height(if (scale) SCALE_HEIGHT else RULER_HEIGHT)
                .onSizeChanged { widthState.floatValue = it.width.toFloat() }
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
                            dragValue = if (ink && !scale) {
                                // The knob follows the finger across the pill, so dragging right raises the value.
                                val usable = (widthState.floatValue - 2 * knobEdgePx).coerceAtLeast(1f)
                                (dragValue + dragAmount * (valueRange.endInclusive - valueRange.start) / usable)
                                    .coerceIn(valueRange)
                            } else {
                                (dragValue - dragAmount / latestPx * step).coerceIn(valueRange)
                            }
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
            if (ink && scale) {
                drawScaleRuler(
                    value = latestValue,
                    range = valueRange,
                    step = step,
                    pxPerStep = latestPx,
                    majorTickEvery = majorTickEvery,
                    labelPaint = labelPaint,
                    labelSizePx = scaleLabelPx,
                )
            } else if (ink) {
                drawInkRuler(latestValue, valueRange, step, majorTickEvery, track)
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
    majorTickEvery: Int,
    track: Brush?,
) {
    val h = size.height
    // The ticks stay put across the pill and the knob moves to the value, so the knob's travel shows the change.
    val edge = h * 0.36f
    val usable = size.width - 2 * edge
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val lastIndex = ((range.endInclusive - range.start) / step).roundToInt().coerceAtLeast(1)
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
    // Same depth as the scale: a light sheen from above, a darker base and a thin lit rim.
    drawRoundRect(brush = INK_SHEEN_BRUSH, cornerRadius = corner)
    drawRoundRect(brush = INK_RIM_BRUSH, cornerRadius = corner, style = Stroke(width = 1.dp.toPx()))

    val tickColor = if (track != null) Color.Black else Color.White
    for (i in 0..lastIndex) {
        val x = edge + usable * i / lastIndex
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
    val knobCentre = Offset(edge + usable * fraction, h / 2f)
    // Soft shadow under the knob lifts it off the pill.
    drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = knobRadius, center = knobCentre + Offset(0f, 2.dp.toPx()))
    drawCircle(color = Color.White, radius = knobRadius, center = knobCentre)
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = knobRadius,
        center = knobCentre,
        style = Stroke(width = 1.dp.toPx()),
    )
}

/**
 * Camera scale: a black pill with ticks along the top and bottom, a number on every major tick, and a red
 * line fixed at the centre. The scale scrolls under the line as the value changes. Ticks and numbers fade
 * towards the pill's ends.
 */
private fun DrawScope.drawScaleRuler(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    pxPerStep: Float,
    majorTickEvery: Int,
    labelPaint: Paint,
    labelSizePx: Float,
) {
    val h = size.height
    val centre = size.width / 2f
    val unitPx = pxPerStep / step
    val centreGrid = (value * GRID_PER_UNIT).roundToInt()
    val minGrid = (range.start * GRID_PER_UNIT).roundToInt()
    val maxGrid = (range.endInclusive * GRID_PER_UNIT).roundToInt()
    val edge = h * 0.1f
    val majorLen = h * 0.26f
    val minorLen = h * 0.11f
    val tickWidth = 1.5.dp.toPx()
    val corner = CornerRadius(h / 2f)

    // Dark pill, lit a little from above and darker towards the bottom, with a faint rim.
    drawRoundRect(brush = SCALE_BODY_BRUSH, cornerRadius = corner)

    labelPaint.textSize = labelSizePx
    val gridPx = unitPx / GRID_PER_UNIT
    val visible = (centre / gridPx).toInt() + 2
    for (i in centreGrid - visible..centreGrid + visible) {
        if (i < minGrid || i > maxGrid) continue
        val x = centre + (i - value * GRID_PER_UNIT) * gridPx
        if (x < 0f || x > size.width) continue
        // Fade towards the ends of the pill, so the scale looks like it curves away.
        val fade = (1f - abs(x - centre) / centre * 0.7f).coerceIn(0.25f, 1f)
        val isMajor = i % GRID_PER_UNIT == 0
        val length = if (isMajor) majorLen else minorLen
        val tickAlpha = fade * if (isMajor) 0.92f else 0.45f
        drawLine(
            color = Color.White.copy(alpha = tickAlpha),
            start = Offset(x, edge),
            end = Offset(x, edge + length),
            strokeWidth = tickWidth,
        )
        drawLine(
            color = Color.White.copy(alpha = tickAlpha),
            start = Offset(x, h - edge - length),
            end = Offset(x, h - edge),
            strokeWidth = tickWidth,
        )
        if (isMajor) {
            labelPaint.color = android.graphics.Color.argb((fade * 255).toInt(), 255, 255, 255)
            drawContext.canvas.nativeCanvas.drawText(
                scaleLabel(i.toFloat() / GRID_PER_UNIT),
                x,
                h / 2f + labelSizePx * 0.35f,
                labelPaint,
            )
        }
    }

    // Red centre line with a soft glow behind it, so it reads as lit rather than flat.
    drawLine(
        color = SCALE_RED.copy(alpha = 0.3f),
        start = Offset(centre, 0f),
        end = Offset(centre, h),
        strokeWidth = 6.dp.toPx(),
    )
    drawLine(
        color = SCALE_RED,
        start = Offset(centre, 0f),
        end = Offset(centre, h),
        strokeWidth = 2.5.dp.toPx(),
    )

    // Rim: a light edge on top and a darker one below, so the pill has a bevel.
    drawRoundRect(brush = SCALE_RIM_BRUSH, cornerRadius = corner, style = Stroke(width = 1.dp.toPx()))
}

/** Whole numbers with a sign, such as "-7" or "+2". Fractions keep one decimal. */
private fun scaleLabel(v: Float): String {
    val n = v.roundToInt()
    return when {
        abs(v - n) < 0.01f && n == 0 -> "0"
        abs(v - n) < 0.01f && n > 0 -> "+$n"
        abs(v - n) < 0.01f -> "$n"
        v > 0f -> "+%.1f".format(v)
        else -> "%.1f".format(v)
    }
}

private fun stepIndex(value: Float, start: Float, step: Float): Int =
    ((value - start) / step).roundToInt()

/** Default readout: the value as a whole percentage of the range, with a sign for bipolar ranges. */
private fun defaultRulerFormat(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val n = (value * 100f).roundToInt()
    return if (range.start < 0f && n > 0) "+$n" else "$n"
}
