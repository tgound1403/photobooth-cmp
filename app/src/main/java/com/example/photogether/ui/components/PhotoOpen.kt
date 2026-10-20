package com.example.photogether.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.photogether.ui.theme.BinsooType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/** A photo's sheet starts this far below the screen and slides up into place. */
private val SLIDE_DISTANCE: Dp = 900.dp

/** Peak bend of the photo, in degrees, around its horizontal and vertical axes. */
private const val BEND_DEG = 7f
private const val SWAY_DEG = 5f

/** Length of the bend that settles the photo after it lands. */
private const val BEND_MS = 1400

private const val CLOSE_MS = 280

/**
 * The motion every opened photo shares: it slides up from below the screen and bends like a real print
 * before it settles. [slide] runs 0 (below the screen) to 1 (in place); [bend] runs 0 to 1 over the settling time.
 */
class PhotoOpenState {
    val slide = Animatable(0f)
    val bend = Animatable(1f)

    /** Slides the photo up and starts its bend. Runs both together; returns when the slide has landed. */
    suspend fun open() = kotlinx.coroutines.coroutineScope {
        launch {
            bend.snapTo(0f)
            bend.animateTo(1f, tween(durationMillis = BEND_MS, easing = LinearEasing))
        }
        slide.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
    }

    /** Slides the photo back down. */
    suspend fun close() {
        slide.animateTo(0f, tween(durationMillis = CLOSE_MS, easing = FastOutLinearInEasing))
    }
}

@Composable
fun rememberPhotoOpenState(): PhotoOpenState = remember { PhotoOpenState() }

/** Damped wave that tilts the photo around its horizontal axis. Zero when the bend is done. */
private fun PhotoOpenState.wave(): Float {
    val b = bend.value
    return sin(b * 2f * PI.toFloat() * 1.5f) * (1f - b)
}

/** Damped wave that sways the photo around its vertical axis. */
private fun PhotoOpenState.sway(): Float {
    val b = bend.value
    return cos(b * 2f * PI.toFloat() * 1.1f) * (1f - b)
}

/** Slides [state] up from below the screen and bends it. Apply to the view that is the opened photo. */
fun Modifier.photoOpenLayer(state: PhotoOpenState): Modifier = graphicsLayer {
    translationY = (1f - state.slide.value) * SLIDE_DISTANCE.toPx()
    rotationX = BEND_DEG * state.wave()
    rotationY = -SWAY_DEG * state.sway()
    // A longer camera distance keeps the tilt gentle, like a real print rather than a card flip.
    cameraDistance = 16f * density
}

/**
 * Full-size photo over a dark scrim, opened like a print slid up from the bottom of the screen with a gentle
 * bend. Tapping the scrim or pressing back slides it away and calls [onDismiss].
 */
@Composable
fun PhotoViewerDialog(
    uri: String,
    takenAt: Long,
    onDismiss: () -> Unit,
) {
    val state = rememberPhotoOpenState()
    val scope = rememberCoroutineScope()
    val caption = remember(takenAt) { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(takenAt)) }
    LaunchedEffect(Unit) { state.open() }
    fun close() {
        scope.launch {
            state.close()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = 0.88f * state.slide.value.coerceIn(0f, 1f)) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .photoOpenLayer(state)
                    .background(Color.White)
                    .padding(10.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    )
                    // A soft highlight that brightens where the print tilts towards the light.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = abs(state.wave()) * 0.35f }
                            .background(
                                Brush.linearGradient(listOf(Color.White, Color.Transparent, Color.Black.copy(alpha = 0.3f))),
                            ),
                    )
                }
                Text(
                    text = caption,
                    color = Color(0xFF555555),
                    style = BinsooType.label,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
