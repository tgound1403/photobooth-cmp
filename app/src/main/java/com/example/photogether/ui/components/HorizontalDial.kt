package com.example.photogether.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.core.camera.Click
import com.example.photogether.core.camera.SliderTickPlayer
import com.example.photogether.core.camera.detentForDrag

private val RidgeSpacing = 6.dp
private val Brass = Color(0xFFD9C27A)

/**
 * A knurled strip you swipe sideways, like the top dials of a rangefinder-style body seen from the side.
 * Dragging right raises [index] by one detent per [pixelsPerDetent]; every detent crossed plays the wheel
 * click and a haptic tick and calls [onIndexChange]. The ridges scroll with the finger. The label sits at the
 * left and the [valueText] in an LCD at the right. A disabled dial is dimmed and ignores input.
 */
@Composable
fun HorizontalDial(
    label: String,
    valueText: String,
    index: Int,
    count: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pixelsPerDetent: Dp = 18.dp,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val pxPerDetent = with(LocalDensity.current) { pixelsPerDetent.toPx() }
    val currentIndex by rememberUpdatedState(index)
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)

    var dragging by remember { mutableStateOf(false) }
    var startIndex by remember { mutableIntStateOf(index) }
    var lastIndex by remember { mutableIntStateOf(index) }
    var dragPx by remember { mutableFloatStateOf(0f) }

    // Finger travel not yet turned into a detent, so the ridges slide smoothly between clicks.
    val residual = if (dragging) {
        (dragPx - (currentIndex - startIndex) * pxPerDetent).coerceIn(-pxPerDetent / 2f, pxPerDetent / 2f)
    } else {
        0f
    }
    val shift = currentIndex * pxPerDetent + residual

    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Brush.verticalGradient(listOf(Color(0xFF020202), Color(0xFF2A2A2E))), shape)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Black, Color.White.copy(alpha = 0.18f))), shape)
            .semantics {
                contentDescription = label
                stateDescription = valueText
            }
            .pointerInput(enabled, count, pxPerDetent) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        startIndex = currentIndex
                        lastIndex = currentIndex
                        dragPx = 0f
                    },
                    onDragEnd = { dragging = false; dragPx = 0f },
                    onDragCancel = { dragging = false; dragPx = 0f },
                ) { change, amount ->
                    change.consume()
                    dragPx += amount
                    val now = detentForDrag(startIndex, dragPx, pxPerDetent, count)
                    if (now != lastIndex) {
                        lastIndex = now
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SliderTickPlayer.play(context, Click.SLIDER)
                        currentOnIndexChange(now)
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize().clip(shape)) { drawKnurl(shift, RidgeSpacing.toPx()) }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .background(Color(0xFF17171A).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = Brass,
                ),
            )
            Box(Modifier.weight(1f))
            ReadoutLcd(text = valueText)
        }
    }
}

/** Vertical ridges across the strip, offset by [shift] pixels; each is a groove with a lit edge, dimmed toward the ends for depth. */
private fun DrawScope.drawKnurl(shift: Float, spacing: Float) {
    val w = size.width
    val h = size.height
    val inset = h * 0.1f
    val stroke = (spacing * 0.32f).coerceAtLeast(1.5f)
    val first = ((shift % spacing) + spacing) % spacing
    var x = first - spacing
    while (x < w + spacing) {
        // Fade near the edges so the ridges seem to curve away round the barrel.
        val edge = (minOf(x, w - x) / (w * 0.5f)).coerceIn(0f, 1f)
        val k = 0.25f + 0.75f * edge
        drawLine(Color.Black.copy(alpha = 0.75f * k), Offset(x, inset), Offset(x, h - inset), strokeWidth = stroke)
        drawLine(Color.White.copy(alpha = 0.34f * k), Offset(x + stroke, inset), Offset(x + stroke, h - inset), strokeWidth = stroke * 0.55f)
        x += spacing
    }
    // Barrel shading: bright band top, dark bottom, plus an edge vignette.
    drawRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.45f))),
    )
    drawRect(
        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f))),
    )
}
