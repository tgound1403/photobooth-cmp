package com.example.cameraxapp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Paper scheme: cream background, black ink. Default for Home, Gallery, Share, Settings, Editor. */
private val PaperColorScheme: ColorScheme = lightColorScheme(
    primary = AppColors.accent,
    onPrimary = AppColors.onAccent,
    primaryContainer = AppColors.surfaceElevated,
    onPrimaryContainer = AppColors.textPrimary,
    secondary = AppColors.textSecondary,
    onSecondary = AppColors.background,
    secondaryContainer = AppColors.surfaceElevated,
    onSecondaryContainer = AppColors.textPrimary,
    tertiary = AppColors.accent,
    onTertiary = AppColors.onAccent,
    background = AppColors.background,
    onBackground = AppColors.textPrimary,
    surface = AppColors.surface,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.surfaceElevated,
    onSurfaceVariant = AppColors.textSecondary,
    surfaceContainerLow = AppColors.background,
    surfaceContainer = AppColors.surface,
    surfaceContainerHigh = AppColors.surfaceElevated,
    surfaceContainerHighest = AppColors.surfaceElevated,
    outline = AppColors.outline,
    outlineVariant = AppColors.outline,
    error = AppColors.danger,
    onError = AppColors.background
)

/** Ink scheme: black background, white ink. Used by the camera and the camera picker. */
private val InkColorScheme: ColorScheme = darkColorScheme(
    primary = InkColors.accent,
    onPrimary = InkColors.onAccent,
    primaryContainer = InkColors.surfaceElevated,
    onPrimaryContainer = InkColors.textPrimary,
    secondary = InkColors.textSecondary,
    onSecondary = InkColors.background,
    secondaryContainer = InkColors.surfaceElevated,
    onSecondaryContainer = InkColors.textPrimary,
    tertiary = InkColors.accent,
    onTertiary = InkColors.onAccent,
    background = InkColors.background,
    onBackground = InkColors.textPrimary,
    surface = InkColors.surface,
    onSurface = InkColors.textPrimary,
    surfaceVariant = InkColors.surfaceElevated,
    onSurfaceVariant = InkColors.textSecondary,
    surfaceContainerLow = InkColors.background,
    surfaceContainer = InkColors.surface,
    surfaceContainerHigh = InkColors.surfaceElevated,
    surfaceContainerHighest = InkColors.surfaceElevated,
    outline = InkColors.outline,
    outlineVariant = InkColors.outline,
    error = InkColors.danger,
    onError = InkColors.background
)

private val BinsooShapes = Shapes(
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg)
)

/** Number of Ink screens currently in the composition (camera, camera picker). Main thread only. */
private var inkScreenCount = 0

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * Applies the system bar icon appearance for the window that hosts [view].
 * Icons are dark (light bar appearance) unless at least one Ink screen is composed.
 * With targetSdk 35 the window is edge-to-edge, so statusBarColor is ignored; only the icons matter.
 */
private fun applySystemBarIcons(view: View) {
    val window = view.context.findActivity()?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)
    val lightBars = inkScreenCount == 0
    controller.isAppearanceLightStatusBars = lightBars
    controller.isAppearanceLightNavigationBars = lightBars
}

/**
 * Keeps the status and navigation bar icons in step with the screen theme.
 * Ink screens are counted so that moving between two Ink screens, or from an Ink screen back to a
 * paper screen, always ends in the right state whatever order enter and dispose happen in.
 * A paper theme re-applies on every recomposition and on entering the composition, and an Ink
 * theme restores the paper state when it leaves.
 */
@Composable
private fun ApplyStatusBar(ink: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, ink) {
        if (ink) inkScreenCount++
        applySystemBarIcons(view)
        onDispose {
            if (ink) inkScreenCount--
            applySystemBarIcons(view)
        }
    }
    SideEffect { applySystemBarIcons(view) }
}

/** The default app theme: paper (cream) scheme with dark status bar icons. */
@Composable
fun PhotogetherTheme(content: @Composable () -> Unit) {
    ApplyStatusBar(ink = false)

    MaterialTheme(
        colorScheme = PaperColorScheme,
        typography = AppTypography,
        shapes = BinsooShapes,
        content = content
    )
}

/** Ink theme for the camera and the camera picker: black background, white status bar icons. */
@Composable
fun InkTheme(content: @Composable () -> Unit) {
    ApplyStatusBar(ink = true)

    MaterialTheme(
        colorScheme = InkColorScheme,
        typography = AppTypography,
        shapes = BinsooShapes,
        content = content
    )
}
