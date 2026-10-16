package com.example.cameraxapp.ui.camera

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp as lerpFloat
import coil.compose.AsyncImage
import com.example.cameraxapp.ui.theme.InkColors
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Stroke
import kotlin.math.roundToInt

/** A freshly taken photo flying from the viewfinder ([from]) into the gallery box ([to]), in window px. */
data class PhotoFlight(val uri: Uri, val from: Rect, val to: Rect)

private const val FLIGHT_MILLIS = 450

/**
 * Animates [flight] from the viewfinder into the gallery box, then calls [onLanded] once. The photo
 * is loaded by Coil, which also applies the JPEG's EXIF orientation. Until it loads, the frame is an
 * empty ink-bordered box.
 */
@Composable
fun PhotoFlightOverlay(flight: PhotoFlight, onLanded: () -> Unit) {
    val progress = remember(flight) { Animatable(0f) }
    LaunchedEffect(flight) {
        progress.animateTo(1f, tween(FLIGHT_MILLIS, easing = FastOutSlowInEasing))
        onLanded()
    }

    val t = progress.value
    val left = lerpFloat(flight.from.left, flight.to.left, t)
    val top = lerpFloat(flight.from.top, flight.to.top, t)
    val width = lerpFloat(flight.from.width, flight.to.width, t)
    val height = lerpFloat(flight.from.height, flight.to.height, t)
    val density = LocalDensity.current
    val corner = lerp(Radius.viewfinder, Radius.sm, t)

    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() },
            )
            .clip(RoundedCornerShape(corner))
            .border(Stroke.border, InkColors.textPrimary, RoundedCornerShape(corner))
            .background(InkColors.surface),
    ) {
        AsyncImage(
            model = flight.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = with(density) { width.toDp() }, height = with(density) { height.toDp() }),
        )
    }
}
