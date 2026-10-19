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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.random.Random

private val WarmTint = Color(0xFFE0903A)

/**
 * One colour look per filter chip, as 4x5 colour matrices (saturation, per-channel gain, lift).
 * The photo is graded with the chip's look, so changing the chip changes the photo's colour.
 */
private val FilmLooks: List<FloatArray> = listOf(
    filmLook(sat = 1.00f, r = 1.05f, g = 1.00f, b = 0.92f, lift = 0.00f),
    filmLook(sat = 0.85f, r = 1.12f, g = 1.00f, b = 0.82f, lift = 0.02f),
    filmLook(sat = 0.70f, r = 0.92f, g = 1.04f, b = 1.08f, lift = 0.00f),
    filmLook(sat = 1.10f, r = 1.08f, g = 0.98f, b = 0.90f, lift = 0.04f),
    filmLook(sat = 0.60f, r = 1.00f, g = 1.00f, b = 1.00f, lift = -0.03f),
)

private fun filmLook(sat: Float, r: Float, g: Float, b: Float, lift: Float): FloatArray {
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f
    val k = 1f - sat
    val o = lift * 255f
    return floatArrayOf(
        r * (lr * k + sat), r * lg * k, r * lb * k, 0f, o,
        g * lr * k, g * (lg * k + sat), g * lb * k, 0f, o,
        b * lr * k, b * lg * k, b * (lb * k + sat), 0f, o,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** The look at chip position [pos], blended between the two neighbouring chips while the selection moves. */
private fun filmLookAt(pos: Float): ColorFilter {
    val i = pos.toInt().coerceIn(0, FilmLooks.lastIndex)
    val j = (i + 1).coerceAtMost(FilmLooks.lastIndex)
    val f = (pos - i).coerceIn(0f, 1f)
    return ColorFilter.colorMatrix(ColorMatrix(FloatArray(20) { AnimMath.lerp(FilmLooks[i][it], FilmLooks[j][it], f) }))
}

/** Vertical drag changes the film strength (tint and grain); the filter strip slides under it. */
@Composable
fun FilmLookIllustration(
    active: Boolean,
    dragLabel: String,
    stripLabel: String,
    modifier: Modifier = Modifier,
) {
    val clock = rememberLoopProgress(active, durationMs = 6200)
    val photos = rememberSamplePhotos(3, 1, 2, 4, 5)
    val grain = remember { List(44) { Offset(Random(it + 11).nextFloat(), Random(it * 7 + 3).nextFloat()) } }
    val dragging by remember { derivedStateOf { clock.value < 0.8f } }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            val t = clock.value
            val strokeW = 2.dp.toPx()
            val vf = Rect(size.width * 0.08f, size.height * 0.02f, size.width * 0.92f, size.height * 0.74f)
            val round = CornerRadius(22.dp.toPx())
            val strength = AnimMath.lerp(
                AnimMath.lerp(0f, 1f, AnimMath.easeInOut(AnimMath.segment(t, 0.1f, 0.4f))),
                0.7f,
                AnimMath.easeInOut(AnimMath.segment(t, 0.5f, 0.75f)),
            )
            val clip = Path().apply { addRoundRect(RoundRect(vf, round)) }

            drawRoundRect(Art.darkGlass, vf.topLeft, vf.size, round)
            clipPath(clip) {
                val image = photos[0]
                if (image != null) {
                    // Original photo underneath; the graded look fades in over it with the strength.
                    drawPhotoCover(image, vf)
                    val sel = 1f + AnimMath.easeInOut(AnimMath.segment(t, 0.82f, 0.95f))
                    drawPhotoCover(image, vf, alpha = strength, colorFilter = filmLookAt(sel))
                } else {
                    drawScene(vf, Art.paper.copy(alpha = 0.9f), Art.yellow, strokeW)
                    drawRect(WarmTint.copy(alpha = 0.4f * strength), vf.topLeft, vf.size)
                }
                grain.forEach { g ->
                    drawCircle(
                        Art.paper.copy(alpha = 0.55f * strength),
                        1.3.dp.toPx(),
                        Offset(vf.left + g.x * vf.width, vf.top + g.y * vf.height),
                    )
                }
            }
            drawRoundRect(Art.ink, vf.topLeft, vf.size, round, style = Stroke(strokeW * 1.5f))

            // Strength track on the right edge with its knob and the dragging touch.
            val trackX = vf.right - 20.dp.toPx()
            val top = vf.top + 30.dp.toPx()
            val bottom = vf.bottom - 30.dp.toPx()
            drawLine(Art.paper.copy(alpha = 0.35f), Offset(trackX, top), Offset(trackX, bottom), strokeW, StrokeCap.Round)
            val knobY = AnimMath.lerp(bottom, top, strength)
            drawLine(Art.yellow, Offset(trackX, bottom), Offset(trackX, knobY), strokeW * 1.6f, StrokeCap.Round)
            drawCircle(Art.yellow, 5.dp.toPx(), Offset(trackX, knobY))
            drawTouch(Offset(trackX - 14.dp.toPx(), knobY), size.width * 0.035f, AnimMath.window(t, 0.05f, 0.8f))

            // Filter strip: the chosen chip moves from the 2nd to the 3rd.
            val sel = 1f + AnimMath.easeInOut(AnimMath.segment(t, 0.82f, 0.95f))
            val chipW = size.width * 0.17f
            val chipH = size.height * 0.15f
            val spacing = chipW + size.width * 0.03f
            val cy = size.height * 0.89f
            for (k in 0 until 5) {
                val x = size.width / 2f + (k - sel) * spacing
                val on = 1f - minOf(abs(k - sel), 1f)
                val topLeft = Offset(x - chipW / 2f, cy - chipH / 2f)
                val fill = androidx.compose.ui.graphics.lerp(Art.card, Art.yellow, on)
                drawRoundRect(fill, topLeft, Size(chipW, chipH), CornerRadius(3.dp.toPx()))
                drawRoundRect(Art.ink, topLeft, Size(chipW, chipH), CornerRadius(3.dp.toPx()), style = Stroke(strokeW))
                val thumb = photos[k]
                if (thumb != null) {
                    drawPhotoCover(
                        thumb,
                        Rect(x - chipW * 0.36f, cy - chipH * 0.34f, x + chipW * 0.36f, cy + chipH * 0.02f),
                        colorFilter = filmLookAt(k.toFloat()),
                    )
                } else {
                    drawCircle(Art.tones[k % Art.tones.size], chipH * 0.22f, Offset(x, cy - chipH * 0.08f))
                }
                drawLine(
                    Art.ink.copy(alpha = 0.5f),
                    Offset(x - chipW * 0.22f, cy + chipH * 0.28f),
                    Offset(x + chipW * 0.22f, cy + chipH * 0.28f),
                    strokeW * 0.8f,
                    StrokeCap.Round,
                )
            }
        }
        GestureTagRow {
            GestureTag(dragLabel, active = dragging)
            GestureTag(stripLabel, active = !dragging)
        }
    }
}
