package com.example.cameraxapp.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import com.example.cameraxapp.core.render.CameraProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SWAY_DEGREES = 25f

/**
 * Camera body drawn on a Canvas, front view, in the profile's colours.
 *
 * Why Canvas and not SceneView: the SceneView version of this composable rendered nothing on the
 * ink background (its GLB materials are near black, 0.05 to 0.2 grey) and, with one Filament engine
 * per pager page, crashed the process with a SIGSEGV inside libgltfio when the picker was left.
 * This drawing is always visible, has no native resources and scales with the width the caller sets.
 *
 * The body sways between -25 and +25 degrees: the width is squashed by cos(angle) and the lens
 * slides sideways by sin(angle), which reads as a yaw turn without a 3D engine.
 * The caller sets the width, for example with Modifier.width(...). The height follows aspectRatio(1.5f).
 */
@Composable
fun CameraBodyArt(profile: CameraProfile, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "camera_body_sway")
    val angle by transition.animateFloat(
        initialValue = -SWAY_DEGREES,
        targetValue = SWAY_DEGREES,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "camera_body_angle",
    )

    val body = displayBodyColor(Color(profile.bodyColor))
    val top = displayTopColor(Color(profile.topColor), body)

    Canvas(modifier = modifier.aspectRatio(1.5f)) {
        val w = size.width
        val h = size.height
        val rad = angle * (PI.toFloat() / 180f)
        val squash = cos(rad).coerceAtLeast(0.6f)
        val lensShift = sin(rad) * w * 0.10f

        scale(scaleX = squash, scaleY = 1f, pivot = Offset(w / 2f, h / 2f)) {
            // Top plate with viewfinder bump, flash and shutter button.
            drawRoundRect(
                color = top,
                topLeft = Offset(w * 0.08f, h * 0.10f),
                size = Size(w * 0.84f, h * 0.30f),
                cornerRadius = CornerRadius(h * 0.07f),
            )
            drawRoundRect(
                color = lerp(top, Color.White, 0.25f),
                topLeft = Offset(w * 0.60f, h * 0.02f),
                size = Size(w * 0.22f, h * 0.14f),
                cornerRadius = CornerRadius(h * 0.03f),
            )
            drawCircle(
                color = Color(0xFFE53935),
                radius = h * 0.055f,
                center = Offset(w * 0.20f, h * 0.10f),
            )

            // Main body.
            drawRoundRect(
                color = body,
                topLeft = Offset(w * 0.04f, h * 0.26f),
                size = Size(w * 0.92f, h * 0.68f),
                cornerRadius = CornerRadius(h * 0.10f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(w * 0.04f, h * 0.26f),
                size = Size(w * 0.92f, h * 0.68f),
                cornerRadius = CornerRadius(h * 0.10f),
                style = Stroke(width = h * 0.015f),
            )

            // Flash window and a small grip stripe.
            drawRoundRect(
                color = lerp(body, Color.White, 0.7f),
                topLeft = Offset(w * 0.11f, h * 0.34f),
                size = Size(w * 0.14f, h * 0.10f),
                cornerRadius = CornerRadius(h * 0.02f),
            )
            drawRoundRect(
                color = lerp(body, Color.Black, 0.25f),
                topLeft = Offset(w * 0.80f, h * 0.40f),
                size = Size(w * 0.10f, h * 0.44f),
                cornerRadius = CornerRadius(h * 0.03f),
            )

            // Lens, drawn last so it can slide with the sway.
            val lensCenter = Offset(w * 0.50f + lensShift, h * 0.60f)
            val lensRadius = h * 0.27f
            drawCircle(color = Color(0xFF1B1B1B), radius = lensRadius, center = lensCenter)
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = lensRadius,
                center = lensCenter,
                style = Stroke(width = h * 0.015f),
            )
            drawCircle(color = Color(0xFF3A3F47), radius = lensRadius * 0.74f, center = lensCenter)
            drawCircle(color = Color(0xFF0B1220), radius = lensRadius * 0.52f, center = lensCenter)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = lensRadius * 0.12f,
                center = lensCenter + Offset(-lensRadius * 0.22f, -lensRadius * 0.22f),
            )
        }
    }
}

/** Dark bodies would vanish on the black picker, so they are lifted toward a light grey. */
private fun displayBodyColor(color: Color): Color =
    if (luminance(color) < 0.35f) lerp(color, Color(0xFFD8D8D8), 0.65f) else color

private fun displayTopColor(color: Color, body: Color): Color =
    if (luminance(color) < 0.35f) lerp(body, Color.Black, 0.25f) else color

private fun luminance(c: Color): Float = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
