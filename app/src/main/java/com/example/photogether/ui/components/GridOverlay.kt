package com.example.photogether.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rule-of-thirds grid (two vertical and two horizontal lines) drawn over the preview.
 * Fills the parent box, so place it in a Box that has the preview's size.
 */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.5f,
    strokeWidth: Dp = 1.dp,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = color.copy(alpha = alpha)
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height
        for (i in 1..2) {
            val x = w * i / 3f
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = stroke,
            )
            val y = h * i / 3f
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = stroke,
            )
        }
    }
}
