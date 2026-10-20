package com.example.photogether.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.core.camera.Click
import com.example.photogether.core.camera.SliderTickPlayer
import com.example.photogether.core.camera.angleDegrees
import com.example.photogether.core.camera.detentAngle
import com.example.photogether.core.camera.detentIndexFor
import com.example.photogether.core.camera.shortestTurn
import com.example.photogether.ui.theme.InkColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RIDGE_COUNT = 48

/** Direction the light comes from, in degrees clockwise from the right: the upper left. */
private const val LIGHT_ANGLE = 225f

private val Graphite = Color(0xFF1B1B1E)
private val GraphiteHigh = Color(0xFF4A4A50)
private val Brass = Color(0xFFD9C27A)

/**
 * A knurled control wheel seen from above, like the command dial of a camera. It turns with a finger
 * (the angle around the centre is what counts, so a thumb can circle it from any side), clicks through
 * [count] detents, keeps turning a little after a flick, then settles into the nearest detent.
 *
 * Depth comes from the layers: a soft shadow under the wheel, a recess in the body it sits in, ridges
 * that shade as they pass the light, a bevelled cap and a brass pointer. Each detent plays a ratchet
 * click and a haptic tick.
 *
 * [index] is the value; turning calls [onIndexChange] for every detent crossed. A disabled wheel
 * is dimmed and does not turn.
 */
@Composable
fun RotaryWheel(
    label: String,
    valueText: String,
    index: Int,
    count: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    degreesPerDetent: Float = 14f,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentIndex by rememberUpdatedState(index)
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)
    val maxAngle = detentAngle((count - 1).coerceAtLeast(0), degreesPerDetent)

    var angle by remember { mutableFloatStateOf(detentAngle(index, degreesPerDetent)) }
    var lastIndex by remember { mutableIntStateOf(index) }
    var touching by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf<Job?>(null) }

    // Moves the wheel to [raw] degrees and clicks once for each detent boundary crossed.
    fun turnTo(raw: Float) {
        angle = raw.coerceIn(0f, maxAngle)
        val now = detentIndexFor(angle, degreesPerDetent, count)
        if (now != lastIndex) {
            lastIndex = now
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            SliderTickPlayer.play(context, Click.WHEEL)
            currentOnIndexChange(now)
        }
    }

    // The value changed from outside (a restored setting, a camera switch): turn the wheel to it.
    LaunchedEffect(index, count) {
        if (touching || index == lastIndex && motion?.isActive == true) return@LaunchedEffect
        lastIndex = index
        animate(
            initialValue = angle,
            targetValue = detentAngle(index, degreesPerDetent),
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        ) { value, _ -> angle = value }
    }

    val press by animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "wheelPress",
    )

    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                contentDescription = label
                stateDescription = valueText
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val s = 1f - 0.035f * press
                    scaleX = s
                    scaleY = s
                }
                .shadow(elevation = (9f - 5f * press).dp, shape = CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .pointerInput(enabled, count) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        motion?.cancel()
                        touching = true
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        var previous = angleDegrees(down.position.x, down.position.y, cx, cy)
                        var previousTime = down.uptimeMillis
                        var velocity = 0f
                        drag(down.id) { change ->
                            val now = angleDegrees(change.position.x, change.position.y, cx, cy)
                            val turn = shortestTurn(previous, now)
                            val dt = (change.uptimeMillis - previousTime).coerceAtLeast(1L)
                            // Low-pass the speed so one jittery sample does not throw the wheel.
                            velocity = velocity * 0.6f + (turn / dt * 1000f) * 0.4f
                            previous = now
                            previousTime = change.uptimeMillis
                            turnTo(angle + turn)
                            change.consume()
                        }
                        touching = false
                        val release = velocity
                        motion = scope.launch {
                            if (kotlin.math.abs(release) > 60f) {
                                AnimationState(initialValue = angle, initialVelocity = release)
                                    .animateDecay(exponentialDecay(frictionMultiplier = 2.6f)) {
                                        turnTo(value)
                                        if (value <= 0f || value >= maxAngle) cancelAnimation()
                                    }
                            }
                            animate(
                                initialValue = angle,
                                targetValue = detentAngle(currentIndex.coerceIn(0, (count - 1).coerceAtLeast(0)), degreesPerDetent),
                                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                            ) { value, _ -> angle = value }
                        }
                    }
                },
        ) {
            Canvas(Modifier.size(size)) { drawWheel(rotation = angle, press = press) }
            Text(
                text = label,
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.13f).sp,
                    letterSpacing = 1.sp,
                    color = Brass,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        ReadoutLcd(text = valueText, modifier = Modifier.padding(top = 5.dp))
    }
}

/** Small recessed display under a control: monospaced amber digits on black glass. */
@Composable
fun ReadoutLcd(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .widthIn(min = 44.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF14130F))), shape)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Black, Color.White.copy(alpha = 0.16f))), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFFFC857),
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

/** Draws the wheel: recess, knurled rim, bevelled cap and pointer. [rotation] is in degrees clockwise. */
private fun DrawScope.drawWheel(rotation: Float, press: Float) {
    val c = center
    val r = this.size.minDimension / 2f

    // Recess: the hole in the camera body the wheel sits in, darker toward the lit side's opposite edge.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF020202), Color(0xFF2A2A2E)),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r),
        ),
        radius = r,
    )

    val wheelR = r * 0.93f
    // Wheel body, with a brushed-metal highlight that stays put while the ridges turn.
    drawCircle(
        brush = Brush.sweepGradient(
            0f to GraphiteHigh, 0.18f to Graphite, 0.38f to GraphiteHigh, 0.5f to Graphite,
            0.68f to GraphiteHigh, 0.86f to Graphite, 1f to GraphiteHigh, center = c,
        ),
        radius = wheelR,
        center = c,
    )

    // Ridges: each shades by where it is now, so the ridges facing the light glint as they pass.
    val ridgeInner = wheelR * 0.82f
    val ridgeOuter = wheelR * 0.985f
    val stroke = (r * 0.045f).coerceAtLeast(1.5f)
    for (i in 0 until RIDGE_COUNT) {
        val deg = rotation + i * 360f / RIDGE_COUNT
        val rad = (deg * PI / 180.0).toFloat()
        val facing = cos((deg - LIGHT_ANGLE) * PI / 180.0).toFloat() // 1 toward the light, -1 away from it
        val dx = cos(rad)
        val dy = sin(rad)
        val from = Offset(c.x + dx * ridgeInner, c.y + dy * ridgeInner)
        val to = Offset(c.x + dx * ridgeOuter, c.y + dy * ridgeOuter)
        // Groove, then the lit edge beside it.
        drawLine(Color.Black.copy(alpha = 0.75f), from, to, strokeWidth = stroke, cap = StrokeCap.Butt)
        val nx = -dy * stroke * 0.9f
        val ny = dx * stroke * 0.9f
        drawLine(
            Color.White.copy(alpha = (0.14f + 0.36f * ((facing + 1f) / 2f)) * (1f - 0.25f * press)),
            Offset(from.x + nx, from.y + ny),
            Offset(to.x + nx, to.y + ny),
            strokeWidth = stroke * 0.55f,
        )
    }

    // Rim bevel above the ridges.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            start = Offset(c.x - wheelR, c.y - wheelR),
            end = Offset(c.x + wheelR, c.y + wheelR),
        ),
        radius = wheelR,
        center = c,
        style = Stroke(width = r * 0.035f),
    )

    // Cap: lit from above, with a dark seam where it meets the ridges.
    val capR = wheelR * 0.64f
    drawCircle(Color.Black.copy(alpha = 0.7f), radius = capR + r * 0.03f, center = c)
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3C3C42), Color(0xFF17171A)),
            startY = c.y - capR,
            endY = c.y + capR,
        ),
        radius = capR,
        center = c,
    )
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
            startY = c.y - capR,
            endY = c.y,
        ),
        radius = capR,
        center = c,
        style = Stroke(width = r * 0.03f),
    )

    // Pointer: a brass dot near the cap's edge that turns with the wheel.
    val pointerRad = ((rotation - 90f) * PI / 180.0).toFloat()
    val pointerAt = Offset(c.x + cos(pointerRad) * capR * 0.78f, c.y + sin(pointerRad) * capR * 0.78f)
    drawCircle(Color.Black.copy(alpha = 0.6f), radius = r * 0.065f, center = pointerAt + Offset(0f, r * 0.012f))
    drawCircle(Brass, radius = r * 0.05f, center = pointerAt)
}
