package com.example.cameraxapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cameraxapp.core.camera.FocusMath
import com.example.cameraxapp.ui.theme.InkColors
import kotlinx.coroutines.delay

private const val SCALE_START = 1.3f
private const val SCALE_MS = 200
private const val FADE_START_MS = 1500L
private const val FADE_MS = 300

/**
 * Yellow focus ring with a horizontal exposure line through its centre, shown at [position] (in
 * the same coordinates as the parent's preview). Each change of [trigger] replays the animation:
 * the ring shrinks from 1.3x to 1x over 200 ms, stays put, then fades out around 1.5 s. Nothing is
 * drawn while [position] is null.
 */
@Composable
fun FocusIndicator(
    position: Offset?,
    trigger: Int,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp,
) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }
    // Skip the animation on first composition; it only plays after a real trigger change.
    val initialTrigger = remember { trigger }

    LaunchedEffect(trigger) {
        if (trigger == initialTrigger) return@LaunchedEffect
        alpha.snapTo(1f)
        scale.snapTo(SCALE_START)
        scale.animateTo(1f, tween(durationMillis = SCALE_MS))
        delay(FADE_START_MS - SCALE_MS)
        alpha.animateTo(0f, tween(durationMillis = FADE_MS))
    }

    if (position == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val a = alpha.value
        if (a <= 0f) return@Canvas

        val rect = FocusMath.indicatorRect(
            x = position.x,
            y = position.y,
            size = sizeDp.toPx(),
            w = this.size.width,
            h = this.size.height,
        )
        val centre = Offset((rect.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f)
        val radius = rect.width / 2f * scale.value
        val strokePx = 1.5.dp.toPx()
        val reach = radius + 8.dp.toPx()

        drawCircle(
            color = InkColors.highlight,
            radius = radius,
            center = centre,
            alpha = a,
            style = Stroke(width = strokePx),
        )
        drawLine(
            color = InkColors.highlight,
            start = Offset(centre.x - reach, centre.y),
            end = Offset(centre.x + reach, centre.y),
            strokeWidth = strokePx,
            alpha = a,
        )
    }
}
