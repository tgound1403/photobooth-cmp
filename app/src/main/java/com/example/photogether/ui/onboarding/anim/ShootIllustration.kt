package com.example.photogether.ui.onboarding.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/**
 * Three beats: press the shutter, the photo flies into the gallery button (iris blinks), then the
 * photo opens in the editor where the tool buttons light up in turn.
 */
@Composable
fun ShootIllustration(
    active: Boolean,
    shootLabel: String,
    galleryLabel: String,
    editLabel: String,
    modifier: Modifier = Modifier,
) {
    val clock = rememberLoopProgress(active, durationMs = 7000)
    val image = rememberSamplePhotos(2).first()
    val beat by remember {
        derivedStateOf {
            val t = clock.value
            when {
                t < 0.26f -> 0
                t < 0.6f -> 1
                else -> 2
            }
        }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            val t = clock.value
            val camAlpha = 1f - AnimMath.segment(t, 0.58f, 0.68f)
            val editAlpha = AnimMath.segment(t, 0.62f, 0.74f)
            if (camAlpha > 0f) drawCamera(t, camAlpha, image)
            if (editAlpha > 0f) drawEditor(t, editAlpha, image)
        }
        GestureTagRow {
            GestureTag(shootLabel, active = beat == 0)
            GestureTag(galleryLabel, active = beat == 1)
            GestureTag(editLabel, active = beat == 2)
        }
    }
}

private fun DrawScope.drawCamera(t: Float, alpha: Float, image: ImageBitmap?) {
    val strokeW = 2.dp.toPx()
    val vf = Rect(size.width * 0.08f, size.height * 0.02f, size.width * 0.92f, size.height * 0.6f)
    val round = CornerRadius(20.dp.toPx())
    drawRoundRect(Art.darkGlass.copy(alpha = alpha), vf.topLeft, vf.size, round)
    if (image != null) {
        // Clip to the rounded viewfinder so the photo's square corners do not show.
        val clip = Path().apply { addRoundRect(RoundRect(vf, round)) }
        clipPath(clip) { drawPhotoCover(image, vf, alpha) }
    } else {
        drawScene(vf, Art.paper.copy(alpha = 0.9f * alpha), Art.yellow.copy(alpha = alpha), strokeW)
    }
    drawRoundRect(Art.ink.copy(alpha = alpha), vf.topLeft, vf.size, round, style = Stroke(strokeW * 1.5f))
    // Flash on capture.
    drawRoundRect(Art.paper.copy(alpha = 0.7f * AnimMath.pulse(t, 0.12f, 0.24f) * alpha), vf.topLeft, vf.size, round)

    // Shutter.
    val shutter = Offset(size.width / 2f, size.height * 0.81f)
    val sr = size.width * 0.085f
    val press = AnimMath.pulse(t, 0.08f, 0.2f)
    drawCircle(Art.ink.copy(alpha = alpha), sr, shutter, style = Stroke(strokeW * 1.5f))
    drawCircle(Art.ink.copy(alpha = alpha), sr * (0.78f - 0.14f * press), shutter)
    drawTouch(Offset(shutter.x + sr * 0.6f, shutter.y + sr * 0.5f), size.width * 0.035f, AnimMath.window(t, 0.04f, 0.22f) * alpha, press)

    // Gallery button: iris that blinks when the photo lands.
    val gallery = Offset(size.width * 0.2f, size.height * 0.81f)
    val gr = size.width * 0.075f
    val landed = AnimMath.segment(t, 0.46f, 0.5f)
    drawCircle(Art.tones[0].copy(alpha = landed * alpha), gr * 0.75f, gallery)
    val blink = AnimMath.pulse(t, 0.46f, 0.58f)
    drawIris(gallery, gr, 1f - 0.8f * blink, Art.ink.copy(alpha = alpha), strokeW)

    // Photo in flight from the viewfinder to the gallery.
    val fly = AnimMath.easeInOut(AnimMath.segment(t, 0.26f, 0.48f))
    if (fly in 0.001f..0.999f) {
        val from = vf.center
        val pos = Offset(AnimMath.lerp(from.x, gallery.x, fly), AnimMath.lerp(from.y, gallery.y, fly))
        val side = AnimMath.lerp(size.width * 0.26f, gr * 1.1f, fly)
        withTransform({ rotate(AnimMath.lerp(-6f, 0f, fly), pos) }) {
            drawRoundRect(Art.card, Offset(pos.x - side / 2f, pos.y - side / 2f), Size(side, side), CornerRadius(3.dp.toPx()))
            val inner = Rect(pos.x - side * 0.42f, pos.y - side * 0.42f, pos.x + side * 0.42f, pos.y + side * 0.28f)
            if (image != null) drawPhotoCover(image, inner) else drawRoundRect(Art.tones[0], inner.topLeft, inner.size)
            drawRoundRect(Art.ink, Offset(pos.x - side / 2f, pos.y - side / 2f), Size(side, side), CornerRadius(3.dp.toPx()), style = Stroke(strokeW))
        }
    }
}

private fun DrawScope.drawEditor(t: Float, alpha: Float, image: ImageBitmap?) {
    val strokeW = 2.dp.toPx()
    val photo = Rect(size.width * 0.17f, size.height * 0.03f, size.width * 0.83f, size.height * 0.62f)
    val corner = CornerRadius(3.dp.toPx())
    if (image != null) {
        drawPhotoCover(image, photo, alpha)
    } else {
        drawRoundRect(Art.tones[0].copy(alpha = alpha), photo.topLeft, photo.size, corner)
        drawScene(photo, Art.paper.copy(alpha = 0.9f * alpha), Art.paper.copy(alpha = alpha), strokeW)
    }
    drawRoundRect(Art.ink.copy(alpha = alpha), photo.topLeft, photo.size, corner, style = Stroke(strokeW * 1.5f))

    val lit = (AnimMath.segment(t, 0.74f, 0.96f) * 4f).toInt().coerceIn(0, 3)
    val btn = size.width * 0.15f
    val gap = size.width * 0.045f
    val rowW = btn * 4 + gap * 3
    val y = size.height * 0.73f
    for (k in 0 until 4) {
        val x = (size.width - rowW) / 2f + k * (btn + gap)
        val on = k == lit && t > 0.74f
        drawRoundRect(
            if (on) Art.yellow.copy(alpha = alpha) else Art.card.copy(alpha = alpha),
            Offset(x, y), Size(btn, btn), corner,
        )
        drawRoundRect(Art.ink.copy(alpha = alpha), Offset(x, y), Size(btn, btn), corner, style = Stroke(strokeW))
        drawToolGlyph(k, Offset(x + btn / 2f, y + btn / 2f), btn * 0.26f, Art.ink.copy(alpha = alpha), strokeW)
    }
}

/** 0 filter (two circles), 1 frame (nested squares), 2 sticker (star), 3 adjust (sliders). */
private fun DrawScope.drawToolGlyph(kind: Int, c: Offset, r: Float, color: androidx.compose.ui.graphics.Color, w: Float) {
    when (kind) {
        0 -> {
            drawCircle(color, r, Offset(c.x - r * 0.45f, c.y), style = Stroke(w))
            drawCircle(color, r, Offset(c.x + r * 0.45f, c.y), style = Stroke(w))
        }
        1 -> {
            drawRect(color, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(w))
            drawRect(color, Offset(c.x - r * 0.5f, c.y - r * 0.5f), Size(r, r), style = Stroke(w))
        }
        2 -> {
            drawLine(color, Offset(c.x, c.y - r * 1.1f), Offset(c.x, c.y + r * 1.1f), w, StrokeCap.Round)
            drawLine(color, Offset(c.x - r * 1.1f, c.y), Offset(c.x + r * 1.1f, c.y), w, StrokeCap.Round)
            drawLine(color, Offset(c.x - r * 0.6f, c.y - r * 0.6f), Offset(c.x + r * 0.6f, c.y + r * 0.6f), w, StrokeCap.Round)
            drawLine(color, Offset(c.x - r * 0.6f, c.y + r * 0.6f), Offset(c.x + r * 0.6f, c.y - r * 0.6f), w, StrokeCap.Round)
        }
        else -> {
            for (i in -1..1) {
                val yy = c.y + i * r * 0.9f
                drawLine(color, Offset(c.x - r, yy), Offset(c.x + r, yy), w, StrokeCap.Round)
                drawCircle(color, w * 1.2f, Offset(c.x + i * r * 0.5f, yy))
            }
        }
    }
}
