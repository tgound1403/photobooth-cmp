package com.example.photogether.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val BLADES = 6
private const val STEP = 2 * PI / BLADES
/** Each blade's outer edge sits this far round from its inner corner, which gives the leaves their slant. */
private const val LEAN = PI / 6
/** How far a leaf edge bulges, as a fraction of the edge length. Set so the leaves curve like the reference iris. */
private const val BLADE_BOW = 0.22f
/** Smallest opening, as a fraction of the radius, so a small hole stays in the centre when closed. */
private const val MIN_OPENING = 0.14f
private const val CLOSE_MILLIS = 150
private const val OPEN_MILLIS = 240
/** Longest wait for the saved photo before the aperture opens anyway. */
private const val SAVE_WAIT_MILLIS = 2_500L

private val IRIS_BACK = Color(0xFF0E0E0E)
private val BLADE_FILL = Color(0xFF262626)
/** Neighbouring leaves alternate shade instead of having outlines, so no hexagon line is drawn. */
private val BLADE_FILL_ALT = Color(0xFF333333)

/**
 * Gallery box in the camera's thumbnail slot, drawn as a camera iris. The last photo shows through the
 * opening. When [captureSeq] changes (the shutter was pressed), the blades close, the new photo is put
 * in, and the blades open again to reveal it. Tapping the box calls [onClick].
 */
@Composable
fun ApertureGalleryButton(
    lastPhoto: Uri?,
    captureSeq: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = remember { Animatable(1f) }
    val latest by rememberUpdatedState(lastPhoto)
    var shown by remember { mutableStateOf(lastPhoto) }

    LaunchedEffect(captureSeq) {
        if (captureSeq == 0) return@LaunchedEffect
        val before = shown
        open.animateTo(0f, tween(CLOSE_MILLIS))
        // The save runs on another thread. Wait for the new photo, but not forever.
        withTimeoutOrNull(SAVE_WAIT_MILLIS) { snapshotFlow { latest }.first { it != before } }
        shown = latest
        open.animateTo(1f, tween(OPEN_MILLIS))
    }

    val t = open.value
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 56.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Mở thư viện" },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Blades and the hexagon clip exist only while the iris is moving; at rest the photo fills the box.
            if (t < 1f) Canvas(modifier = Modifier.fillMaxSize()) { drawBlades(t) }
            val photo = shown
            if (photo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (t < 1f) Modifier.clip(apertureShape(t)) else Modifier),
                ) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (t > 0.5f) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    tint = InkColors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Opening radius as a fraction of the iris radius: full when open, and a small centre hole left when closed. */
private fun openingScale(open: Float): Float = MIN_OPENING + (1f - MIN_OPENING) * open

/** The blades turn as they close, so the opening rotates a little. */
private fun twist(open: Float): Double = (1f - open) * (PI / 9)

private fun vertex(center: Offset, radius: Float, angle: Double): Offset =
    Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())

/** The hexagonal opening the photo is clipped to. */
private fun apertureShape(open: Float) = GenericShape { size, _ ->
    val center = Offset(size.width / 2f, size.height / 2f)
    // Same outer radius as the blades, so the hole matches the blade ring exactly.
    val radius = sqrt(size.width * size.width + size.height * size.height) / 2f * openingScale(open)
    val rot = twist(open)
    for (k in 0 until BLADES) {
        val p = vertex(center, radius, rot + k * STEP)
        if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/**
 * Control point for a leaf edge from [from] to [to]. It sits off the chord, on the side away from the
 * blade's [centroid], so the edge bulges outward and the leaf looks convex like a real iris blade.
 */
private fun bow(from: Offset, to: Offset, centroid: Offset): Offset {
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val left = Offset(mid.x - dy * BLADE_BOW, mid.y + dx * BLADE_BOW)
    val right = Offset(mid.x + dy * BLADE_BOW, mid.y - dx * BLADE_BOW)
    return if ((left - centroid).getDistanceSquared() > (right - centroid).getDistanceSquared()) left else right
}

/** Dark disc with six slanted, slightly curved blades around the opening. */
private fun DrawScope.drawBlades(open: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    // Half the box diagonal, so the blades reach past the corners and their straight outer ends stay out of view.
    val outer = sqrt(size.width * size.width + size.height * size.height) / 2f
    val inner = outer * openingScale(open)
    val rot = twist(open)
    drawCircle(IRIS_BACK, radius = outer, center = center)
    for (k in 0 until BLADES) {
        val a0 = rot + k * STEP
        val a1 = a0 + STEP
        val innerStart = vertex(center, inner, a0)
        val innerEnd = vertex(center, inner, a1)
        val outerEnd = vertex(center, outer, a1 + LEAN)
        val outerStart = vertex(center, outer, a0 + LEAN)
        // Both leaf edges bulge outward from the leaf's own centre, so each blade curves like a real iris leaf.
        val centroid = Offset(
            (innerStart.x + innerEnd.x + outerEnd.x + outerStart.x) / 4f,
            (innerStart.y + innerEnd.y + outerEnd.y + outerStart.y) / 4f,
        )
        val bowOut = bow(innerEnd, outerEnd, centroid)
        val bowBack = bow(outerStart, innerStart, centroid)
        val blade = Path().apply {
            moveTo(innerStart.x, innerStart.y)
            lineTo(innerEnd.x, innerEnd.y)
            quadraticBezierTo(bowOut.x, bowOut.y, outerEnd.x, outerEnd.y)
            lineTo(outerStart.x, outerStart.y)
            quadraticBezierTo(bowBack.x, bowBack.y, innerStart.x, innerStart.y)
            close()
        }
        drawPath(blade, if (k % 2 == 0) BLADE_FILL else BLADE_FILL_ALT)
    }
}
