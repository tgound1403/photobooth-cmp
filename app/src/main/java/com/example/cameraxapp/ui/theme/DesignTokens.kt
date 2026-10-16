package com.example.cameraxapp.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/** Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 dp. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner radius scale: almost square. 2 / 6 / 10 dp. Camera viewfinder uses [viewfinder]. */
object Radius {
    val sm = 2.dp
    val md = 6.dp
    val lg = 10.dp

    /** Camera viewfinder corners. */
    val viewfinder = 28.dp
}

/** Icon and touch-target sizes. */
object IconSize {
    val xs = 16.dp
    val sm = 20.dp
    val md = 24.dp
    val lg = 32.dp
    /** Touch target for round icon buttons. */
    val xl = 40.dp
    /** Hero icons on empty and error states. */
    val hero = 64.dp
}

/** Stroke widths. */
object Stroke {
    val hairline = 1.dp
    val selected = 3.dp

    /** Black ink border for cards, photo tiles and outlined buttons. */
    val border = 2.dp
}

/** Motion tokens. */
object Motion {
    val DefaultSpring: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 400f)

    const val DurationShort = 150
    const val DurationMedium = 250
    const val DurationLong = 400

    /** Scale applied to a card while it is pressed. */
    const val PressedScale = 0.97f
}

/**
 * Deprecated. Use [Radius]. Kept only for out-of-scope callers, if any.
 */
@Deprecated("Use Radius", ReplaceWith("Radius", "com.example.cameraxapp.ui.theme.Radius"))
object CornerRadius {
    val card = Radius.lg
    val button = Radius.md
}
