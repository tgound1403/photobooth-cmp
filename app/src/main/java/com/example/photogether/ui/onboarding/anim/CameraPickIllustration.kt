package com.example.photogether.ui.onboarding.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Polaroid-style camera cards sliding past under a swiping touch point. */
@Composable
fun CameraPickIllustration(active: Boolean, modifier: Modifier = Modifier) {
    val clock = rememberLoopProgress(active, durationMs = 5200)
    val photos = rememberSamplePhotos(1, 2, 3, 4)
    Canvas(modifier.fillMaxWidth().aspectRatio(1.15f)) {
        val t = clock.value
        val slide = AnimMath.easeInOut(AnimMath.segment(t, 0.12f, 0.42f)) +
            AnimMath.easeInOut(AnimMath.segment(t, 0.55f, 0.85f))
        val cardW = size.width * 0.44f
        val cardH = cardW * 1.22f
        val spacing = size.width * 0.36f
        val center = Offset(size.width / 2f, size.height * 0.46f)

        // Farthest cards first so the centred one lands on top.
        (0 until Art.tones.size).sortedByDescending { abs(it - slide) }.forEach { i ->
            val d = i - slide
            val near = 1f - minOf(abs(d), 1f)
            val c = Offset(center.x + d * spacing, center.y + (1f - near) * cardH * 0.04f)
            withTransform({
                rotate(d.coerceIn(-1.5f, 1.5f) * 6f, c)
                scale(AnimMath.lerp(0.78f, 1f, near), AnimMath.lerp(0.78f, 1f, near), c)
            }) {
                drawPolaroid(c, cardW, cardH, i, selected = AnimMath.segment(near, 0.5f, 1f), photos[i])
            }
        }

        // Swipe touches: one per slide.
        fun swipe(from: Float, to: Float, startX: Float, endX: Float) {
            val p = AnimMath.easeInOut(AnimMath.segment(t, from, to))
            drawTouch(
                Offset(AnimMath.lerp(startX, endX, p), center.y + cardH * 0.66f),
                size.width * 0.04f,
                AnimMath.window(t, from - 0.04f, to + 0.02f),
            )
        }
        swipe(0.12f, 0.42f, center.x + spacing * 0.5f, center.x - spacing * 0.5f)
        swipe(0.55f, 0.85f, center.x + spacing * 0.5f, center.x - spacing * 0.5f)
    }
}

private fun DrawScope.drawPolaroid(center: Offset, w: Float, h: Float, index: Int, selected: Float, image: ImageBitmap?) {
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val border = 2.dp.toPx()
    val corner = CornerRadius(3.dp.toPx())
    if (selected > 0f) {
        val halo = 5.dp.toPx() * selected
        drawRoundRect(
            Art.yellow.copy(alpha = selected),
            Offset(left - halo, top - halo),
            Size(w + halo * 2, h + halo * 2),
            CornerRadius(3.dp.toPx() + halo),
        )
    }
    drawRoundRect(Art.card, Offset(left, top), Size(w, h), corner)
    drawRoundRect(Art.ink, Offset(left, top), Size(w, h), corner, style = Stroke(border))

    val pad = w * 0.08f
    val photo = Rect(left + pad, top + pad, left + w - pad, top + pad + (w - pad * 2) * 0.95f)
    if (image != null) {
        drawPhotoCover(image, photo)
    } else {
        drawRect(Art.tones[index % Art.tones.size], photo.topLeft, photo.size)
        drawScene(photo, Art.paper.copy(alpha = 0.85f), Art.paper, border * 0.8f)
    }
    drawRect(Art.ink, photo.topLeft, photo.size, style = Stroke(border))

    // Caption lines under the photo.
    val ly = photo.bottom + (top + h - photo.bottom) * 0.38f
    drawLine(Art.ink, Offset(left + pad, ly), Offset(left + w * 0.62f, ly), border, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(
        Art.ink.copy(alpha = 0.4f),
        Offset(left + pad, ly + border * 3f),
        Offset(left + w * 0.4f, ly + border * 3f),
        border,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}
