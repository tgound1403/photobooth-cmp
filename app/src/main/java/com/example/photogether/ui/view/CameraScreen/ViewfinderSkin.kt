package com.example.photogether.ui.view.CameraScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.photogether.core.render.ViewfinderSkin
import com.example.photogether.ui.theme.BinsooType

/** Shutter fill for the skin, or [Color.Unspecified] to keep the default. */
val ViewfinderSkin.shutterColor: Color
    get() = if (shutterTint == 0L) Color.Unspecified else Color(shutterTint)

/** Shutter ring colour, or [Color.Unspecified] for no ring. */
val ViewfinderSkin.shutterRingColor: Color
    get() = if (shutterRing == 0L) Color.Unspecified else Color(shutterRing)

private val WarmOffWhite = Color(0xFFF2E8D5)
private val FrameOrange = Color(0xFFFF8A2B)

/**
 * Draws the skin over the live preview. Draw only: it takes no input, so touches reach the
 * preview and the gestures under it.
 */
@Composable
fun ViewfinderSkinOverlay(skin: ViewfinderSkin, modifier: Modifier = Modifier) {
    when (skin) {
        ViewfinderSkin.NONE -> Unit
        ViewfinderSkin.FILM_FRAME -> Box(modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 10.dp.toPx()
                drawRect(
                    color = WarmOffWhite.copy(alpha = 0.85f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            Text(
                text = "24A",
                style = BinsooType.micro,
                color = FrameOrange,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp),
            )
        }
        ViewfinderSkin.RANGEFINDER -> Canvas(modifier.fillMaxSize()) { drawRangefinder(Color.White.copy(alpha = 0.7f)) }
        ViewfinderSkin.RANGEFINDER_MONO -> Canvas(modifier.fillMaxSize()) {
            drawRangefinder(Color(0xFFB0B0B0).copy(alpha = 0.85f))
            drawRect(
                color = Color(0xFF9A9A9A).copy(alpha = 0.6f),
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        ViewfinderSkin.SOFT_GLASS -> Canvas(modifier.fillMaxSize()) {
            val radius = maxOf(size.width, size.height) * 0.75f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFB347).copy(alpha = 0.12f), Color.Black.copy(alpha = 0.35f)),
                    center = center,
                    radius = radius,
                ),
            )
        }
        ViewfinderSkin.LCD_DIGICAM -> Box(modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 4.dp.toPx().coerceAtLeast(4f)
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.Black.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 52.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape))
                Text("REC", style = BinsooType.micro, color = Color(0xFFE53935))
            }
        }
    }
}

private fun DrawScope.drawRangefinder(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx())
    val m = 16.dp.toPx()
    val len = 22.dp.toPx()
    val w = size.width
    val h = size.height
    fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(cx, cy), Offset(cx + dx * len, cy), strokeWidth = stroke.width)
        drawLine(color, Offset(cx, cy), Offset(cx, cy + dy * len), strokeWidth = stroke.width)
    }
    corner(m, m, 1f, 1f)
    corner(w - m, h - m, -1f, -1f)
    val fw = w * 0.22f
    val fh = fw * 0.75f
    drawRect(color, Offset(center.x - fw / 2, center.y - fh / 2), Size(fw, fh), style = stroke)
}
