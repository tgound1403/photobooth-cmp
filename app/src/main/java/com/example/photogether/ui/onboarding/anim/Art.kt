package com.example.photogether.ui.onboarding.anim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.photogether.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shared drawing primitives for the onboarding line-art. Ink on paper, yellow for emphasis. */
internal object Art {
    val ink = Color(0xFF0B0B0B)
    val paper get() = AppColors.background
    val card get() = AppColors.surface
    val yellow get() = AppColors.highlight
    val darkGlass = Color(0xFF111111)

    /** Film-stock tints used for the "photos" in the illustrations. */
    val tones = listOf(Color(0xFFC9A66B), Color(0xFF5E8C8A), Color(0xFFC97B7B), Color(0xFF8A8F5A))
}

/**
 * Aperture: a ring with a hexagonal hole. [open] 0 is shut, 1 is wide open; the blades turn
 * as it moves. Blade edges are drawn as lines so it still reads as an iris when almost closed.
 */
internal fun DrawScope.drawIris(center: Offset, radius: Float, open: Float, color: Color, stroke: Float) {
    val hole = radius * (0.12f + 0.78f * open.coerceIn(0f, 1f))
    val turn = (1f - open) * 60f
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(androidx.compose.ui.geometry.Rect(center, radius))
        val hex = hexagon(center, hole, turn)
        addPath(hex)
    }
    drawPath(ring, color)
    // Blade seams.
    for (k in 0 until 6) {
        val a = Math.toRadians((turn + 60.0 * k))
        val inner = Offset(center.x + hole * cos(a).toFloat(), center.y + hole * sin(a).toFloat())
        val b = a + PI / 3.2
        val outer = Offset(center.x + radius * cos(b).toFloat(), center.y + radius * sin(b).toFloat())
        drawLine(Art.paper.copy(alpha = 0.55f), inner, outer, stroke * 0.6f, StrokeCap.Round)
    }
}

internal fun hexagon(center: Offset, r: Float, turnDeg: Float): Path = Path().apply {
    for (k in 0 until 6) {
        val a = Math.toRadians((turnDeg + 60.0 * k))
        val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** A touch point: dark dot with a paper rim, plus an expanding ring while pressed. */
internal fun DrawScope.drawTouch(center: Offset, radius: Float, alpha: Float, press: Float = 0f) {
    if (alpha <= 0f) return
    if (press > 0f) {
        drawCircle(
            Art.paper.copy(alpha = (1f - press) * alpha),
            radius * (1.2f + 1.6f * press),
            center,
            style = Stroke(radius * 0.18f),
        )
    }
    drawCircle(Art.paper.copy(alpha = alpha), radius * 1.08f, center)
    drawCircle(Art.ink.copy(alpha = alpha), radius, center)
}

/** Line-art scene shown through the viewfinder: sun, hills and a small house. */
internal fun DrawScope.drawScene(
    area: androidx.compose.ui.geometry.Rect,
    line: Color,
    sun: Color,
    stroke: Float,
    offset: Offset = Offset.Zero,
) {
    val w = area.width
    val h = area.height
    fun p(x: Float, y: Float) = Offset(area.left + w * x + offset.x, area.top + h * y + offset.y)
    drawCircle(sun, w * 0.09f, p(0.7f, 0.3f))
    val far = Path().apply {
        moveTo(p(0f, 0.72f).x, p(0f, 0.72f).y)
        cubicTo(p(0.2f, 0.52f).x, p(0.2f, 0.52f).y, p(0.38f, 0.56f).x, p(0.38f, 0.56f).y, p(0.55f, 0.68f).x, p(0.55f, 0.68f).y)
        cubicTo(p(0.7f, 0.58f).x, p(0.7f, 0.58f).y, p(0.9f, 0.6f).x, p(0.9f, 0.6f).y, p(1f, 0.66f).x, p(1f, 0.66f).y)
    }
    drawPath(far, line, style = Stroke(stroke, cap = StrokeCap.Round))
    val near = Path().apply {
        moveTo(p(0f, 0.86f).x, p(0f, 0.86f).y)
        cubicTo(p(0.3f, 0.74f).x, p(0.3f, 0.74f).y, p(0.6f, 0.82f).x, p(0.6f, 0.82f).y, p(1f, 0.78f).x, p(1f, 0.78f).y)
    }
    drawPath(near, line, style = Stroke(stroke, cap = StrokeCap.Round))
    // House.
    val hx = 0.22f
    val body = Path().apply {
        moveTo(p(hx, 0.8f).x, p(hx, 0.8f).y)
        lineTo(p(hx, 0.66f).x, p(hx, 0.66f).y)
        lineTo(p(hx + 0.07f, 0.58f).x, p(hx + 0.07f, 0.58f).y)
        lineTo(p(hx + 0.14f, 0.66f).x, p(hx + 0.14f, 0.66f).y)
        lineTo(p(hx + 0.14f, 0.78f).x, p(hx + 0.14f, 0.78f).y)
    }
    drawPath(body, line, style = Stroke(stroke, cap = StrokeCap.Round))
}

internal fun DrawScope.scaledAbout(pivot: Offset, factor: Float, block: DrawScope.() -> Unit) {
    withTransform({
        scale(factor, factor, pivot)
    }, block)
}

internal fun DrawScope.tilted(pivot: Offset, degrees: Float, block: DrawScope.() -> Unit) =
    rotate(degrees, pivot, block)
