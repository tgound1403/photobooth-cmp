package com.example.photogether.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.example.photogether.core.tutorial.TutorialScreen
import com.example.photogether.core.tutorial.TutorialStep
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.tgound.photogether.R
import kotlinx.coroutines.delay

/** Rectangles of the controls the tour can point at, by target name, in window coordinates. */
class TutorialHighlights {
    val rects = mutableStateMapOf<String, Rect>()
}

private val LocalTutorialHighlights = staticCompositionLocalOf<TutorialHighlights?> { null }

/** Marks a control as the spotlight target named [id]. Does nothing outside a [TutorialLayer]. */
fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val highlights = LocalTutorialHighlights.current
    if (highlights == null) this
    else onGloballyPositioned { highlights.rects[id] = it.boundsInRoot() }
}

/**
 * Shows the tour's card and spotlight over [content] when [step] belongs to [screen]. The overlay has no pointer
 * input of its own, so taps on the spotlit control, and on everything else, reach the screen underneath.
 */
@Composable
fun TutorialLayer(
    step: TutorialStep?,
    screen: TutorialScreen,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    content: @Composable () -> Unit,
) {
    val highlights = remember { TutorialHighlights() }
    val active = step?.takeIf { it.screen == screen }
    CompositionLocalProvider(LocalTutorialHighlights provides highlights) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (active != null) {
                if (active.isTip) {
                    // A tip dismisses itself, so it never traps a user who ignores it.
                    LaunchedEffect(active) {
                        delay(TutorialStep.TIP_MILLIS)
                        onNext()
                    }
                }
                TutorialOverlay(
                    step = active,
                    rect = active.target?.let { highlights.rects[it] },
                    onNext = onNext,
                    onSkip = onSkip,
                )
            }
        }
    }
}

@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    rect: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    // Window origin and height of this overlay, so the window-space target rect can be drawn in local space.
    var origin by remember { mutableStateOf(Offset.Zero) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var widthPx by remember { mutableFloatStateOf(0f) }
    val ringAlpha by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "ringAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInRoot()
                heightPx = it.size.height.toFloat()
                widthPx = it.size.width.toFloat()
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            // On the camera the live view must stay clear to shoot, so only a ring marks the control.
            val dim = step.screen != TutorialScreen.CAMERA
            if (dim) drawRect(Color.Black.copy(alpha = 0.72f))
            rect?.let { r ->
                val pad = 8.dp.toPx()
                val topLeft = Offset(r.left - origin.x - pad, r.top - origin.y - pad)
                val size = Size(r.width + pad * 2, r.height + pad * 2)
                val corner = CornerRadius(Radius.lg.toPx())
                if (dim) {
                    drawRoundRect(Color.Transparent, topLeft, size, corner, blendMode = BlendMode.Clear)
                } else {
                    drawRoundRect(
                        color = Color.White.copy(alpha = ringAlpha),
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = corner,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }
        TutorialCard(
            step = step,
            // Below the midline the spotlit control sits at the bottom, so the card goes to the top, and the reverse.
            // Next to the spotlit control: above it when it is in the lower half, below it otherwise.
            atTop = rect != null && rect.center.y < origin.y + heightPx / 2f,
            anchorPx = rect?.let {
                if (it.center.y < origin.y + heightPx / 2f) it.bottom - origin.y else heightPx - (it.top - origin.y)
            },
            // Held sideways the controls sit on the left rail and the right plate; the card stays in the viewfinder between.
            controlsOnRight = if (landscape && rect != null) rect.center.x > origin.x + widthPx / 2f else null,
            onNext = onNext,
            onSkip = onSkip,
        )
    }
}

@Composable
private fun TutorialCard(
    step: TutorialStep,
    atTop: Boolean,
    anchorPx: Float?,
    controlsOnRight: Boolean?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val (titleRes, bodyRes) = copyFor(step)
    val shape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    val gap = Spacing.md
    val statusBar = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    // Distance from the screen edge to the card: right against the control, never under a system bar.
    val edge = with(density) { anchorPx?.toDp() }?.plus(gap)
    val sideways = controlsOnRight != null
    val topPad = if (atTop && !sideways) maxOf(edge ?: 0.dp, statusBar + Spacing.lg) else statusBar + Spacing.lg
    val bottomPad = if (!atTop || sideways) {
        if (sideways) navBar + Spacing.lg else maxOf(edge ?: 0.dp, navBar + Spacing.lg)
    } else navBar + Spacing.lg
    // Widths of the left rail and the right plate, so a sideways card clears both.
    val startPad = if (controlsOnRight == false) 96.dp else Spacing.lg
    val endPad = if (controlsOnRight == true) 300.dp else Spacing.lg
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = startPad, end = endPad, top = topPad, bottom = bottomPad),
        verticalArrangement = if (atTop && !sideways) Arrangement.Top else Arrangement.Bottom,
        horizontalAlignment = if (controlsOnRight == false) Alignment.Start else if (controlsOnRight == true) Alignment.End else Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.95f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "${step.ordinal + 1}/${TutorialStep.entries.size}",
                style = BinsooType.label,
                color = InkColors.textSecondary,
            )
            Text(
                text = stringResource(titleRes),
                style = BinsooType.title,
                color = InkColors.textPrimary,
            )
            Text(
                text = stringResource(bodyRes),
                style = BinsooType.label,
                color = InkColors.textPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tutorial_skip).uppercase(),
                    style = BinsooType.label,
                    color = InkColors.textSecondary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSkip,
                    ),
                )
                if (step.hasNextButton) {
                    Text(
                        text = stringResource(
                            if (step == TutorialStep.FINISH) R.string.tutorial_done else R.string.tutorial_next,
                        ).uppercase(),
                        style = BinsooType.label,
                        color = InkColors.accent,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNext,
                        ),
                    )
                }
            }
        }
    }
}

/** Steps with nothing to do but read get a button; the rest move on when the user does the action. */
private val TutorialStep.hasNextButton: Boolean
    get() = isPassive

private fun copyFor(step: TutorialStep): Pair<Int, Int> = when (step) {
    TutorialStep.HOME_CAMERA -> R.string.tutorial_home_camera_title to R.string.tutorial_home_camera_body
    TutorialStep.CAPTURE -> R.string.tutorial_capture_title to R.string.tutorial_capture_body
    TutorialStep.FIRST_SHOT -> R.string.tutorial_first_title to R.string.tutorial_first_body
    TutorialStep.LOOK -> R.string.tutorial_look_title to R.string.tutorial_look_body
    TutorialStep.ADJUST -> R.string.tutorial_adjust_title to R.string.tutorial_adjust_body
    TutorialStep.OPEN_GALLERY -> R.string.tutorial_gallery_title to R.string.tutorial_gallery_body
    TutorialStep.SHOOT_MORE -> R.string.tutorial_shoot_title to R.string.tutorial_shoot_body
    TutorialStep.OPEN_CALENDAR_DAY -> R.string.tutorial_calendar_title to R.string.tutorial_calendar_body
    TutorialStep.FINISH -> R.string.tutorial_finish_title to R.string.tutorial_finish_body
}
