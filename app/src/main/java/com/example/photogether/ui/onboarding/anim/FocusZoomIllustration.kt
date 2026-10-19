package com.example.photogether.ui.onboarding.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.photogether.ui.theme.Spacing

/** Viewfinder: a tap pulls the scene into focus, then two touch points pinch to zoom. */
@Composable
fun FocusZoomIllustration(
    active: Boolean,
    tapLabel: String,
    pinchLabel: String,
    modifier: Modifier = Modifier,
) {
    val clock = rememberLoopProgress(active, durationMs = 6000)
    val photo = rememberSamplePhotos(2).first()
    val zoomed by remember { derivedStateOf { clock.value > 0.7f } }
    val tapping by remember { derivedStateOf { clock.value < 0.52f } }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            Canvas(Modifier.matchParentSize()) {
                val t = clock.value
                val vf = Rect(size.width * 0.08f, size.height * 0.02f, size.width * 0.92f, size.height * 0.98f)
                val round = CornerRadius(22.dp.toPx())
                val strokeW = 2.dp.toPx()
                val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(vf, round)) }

                drawRoundRect(Art.darkGlass, vf.topLeft, vf.size, round)
                val zoom = 1f + 0.7f * AnimMath.easeInOut(AnimMath.segment(t, 0.58f, 0.82f))
                val focus = AnimMath.easeOutCubic(AnimMath.segment(t, 0.2f, 0.4f))
                clipPath(clip) {
                    withTransform({ scale(zoom, zoom, vf.center) }) {
                        if (photo != null) {
                            // Before focus the photo is shifted and faint, like a soft double image.
                            if (focus < 1f) {
                                drawPhotoCover(photo, vf.translate(Offset((1f - focus) * 9.dp.toPx(), (1f - focus) * 5.dp.toPx())), alpha = 0.5f * (1f - focus))
                            }
                            drawPhotoCover(photo, vf, alpha = 0.55f + 0.45f * focus)
                        } else {
                            if (focus < 1f) {
                                drawScene(vf, Art.paper.copy(alpha = 0.35f * (1f - focus)), Art.yellow.copy(alpha = 0.35f * (1f - focus)), strokeW, Offset((1f - focus) * 9.dp.toPx(), (1f - focus) * 5.dp.toPx()))
                            }
                            drawScene(vf, Art.paper.copy(alpha = 0.55f + 0.45f * focus), Art.yellow, strokeW)
                        }
                    }
                }
                drawRoundRect(Art.ink, vf.topLeft, vf.size, round, style = Stroke(strokeW * 1.5f))

                // Focus square at the tapped point.
                val tap = Offset(vf.center.x + vf.width * 0.14f, vf.center.y - vf.height * 0.02f)
                val boxAlpha = AnimMath.window(t, 0.2f, 0.62f)
                if (boxAlpha > 0f) {
                    val side = vf.width * 0.17f * AnimMath.lerp(1.7f, 1f, AnimMath.easeOutCubic(AnimMath.segment(t, 0.2f, 0.36f)))
                    drawRect(
                        Art.yellow.copy(alpha = boxAlpha),
                        Offset(tap.x - side / 2f, tap.y - side / 2f),
                        Size(side, side),
                        style = Stroke(strokeW * 1.4f),
                    )
                }
                drawTouch(tap, size.width * 0.035f, AnimMath.window(t, 0.1f, 0.34f), AnimMath.segment(t, 0.18f, 0.32f))

                // Pinch.
                val gap = AnimMath.lerp(vf.width * 0.05f, vf.width * 0.2f, AnimMath.easeInOut(AnimMath.segment(t, 0.58f, 0.82f)))
                val pinchAlpha = AnimMath.window(t, 0.54f, 0.9f)
                val dir = Offset(0.8f, -0.6f)
                val c = Offset(vf.center.x, vf.center.y + vf.height * 0.06f)
                drawTouch(Offset(c.x - dir.x * gap, c.y - dir.y * gap), size.width * 0.035f, pinchAlpha)
                drawTouch(Offset(c.x + dir.x * gap, c.y + dir.y * gap), size.width * 0.035f, pinchAlpha)
            }
            Text(
                text = if (zoomed) "2x" else "1x",
                style = MaterialTheme.typography.labelLarge,
                color = Art.paper,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.lg)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(Art.ink)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }
        GestureTagRow {
            GestureTag(tapLabel, active = tapping)
            GestureTag(pinchLabel, active = !tapping)
        }
    }
}
