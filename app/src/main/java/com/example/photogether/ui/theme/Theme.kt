package com.example.photogether.ui.theme

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Whether the paper screens use [DarkPaper]. Provided once at the root from the dark theme setting. */
val LocalDarkThemeEnabled = staticCompositionLocalOf { false }

/**
 * Paper scheme for Home, Gallery, Share, Settings and Editor. [dark] picks the Material base
 * (light or dark) and [palette] supplies the values, so both modes share one mapping.
 */
private fun paperColorScheme(palette: PaperPalette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.surfaceElevated,
        onPrimaryContainer = palette.textPrimary,
        secondary = palette.textSecondary,
        onSecondary = palette.background,
        secondaryContainer = palette.surfaceElevated,
        onSecondaryContainer = palette.textPrimary,
        tertiary = palette.accent,
        onTertiary = palette.onAccent,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,
        surfaceContainerLow = palette.background,
        surfaceContainer = palette.surface,
        surfaceContainerHigh = palette.surfaceElevated,
        surfaceContainerHighest = palette.surfaceElevated,
        outline = palette.outline,
        outlineVariant = palette.outline,
        error = palette.danger,
        onError = palette.background
    )
}

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

/** Whether the most recently composed paper screen is dark. Main thread only. */
private var paperDark = false

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * Applies the system bar icon appearance for the window that hosts [view].
 * Icons are dark (light bar appearance) only on a light paper screen with no Ink screen composed.
 * With targetSdk 35 the window is edge-to-edge, so statusBarColor is ignored; only the icons matter.
 */
private fun applySystemBarIcons(view: View) {
    val window = view.context.findActivity()?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)
    val lightBars = inkScreenCount == 0 && !paperDark
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
private fun ApplyStatusBar(ink: Boolean, dark: Boolean) {
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
    SideEffect {
        if (!ink) paperDark = dark
        applySystemBarIcons(view)
    }
}

/**
 * The default app theme: paper scheme. Cream by default, warm dark when [LocalDarkThemeEnabled]
 * is set. Status bar icons follow the palette.
 */
@Composable
fun PhotogetherTheme(content: @Composable () -> Unit) {
    val dark = LocalDarkThemeEnabled.current
    val palette = if (dark) DarkPaper else LightPaper
    ApplyStatusBar(ink = false, dark = dark)

    CompositionLocalProvider(LocalPaperPalette provides palette) {
        MaterialTheme(
            colorScheme = paperColorScheme(palette, dark),
            typography = AppTypography,
            shapes = BinsooShapes,
            content = content
        )
    }
}

/**
 * Ink theme for the camera, the camera picker and the photo booth capture and edit screens:
 * black background, white status bar icons. Always dark, whatever the dark theme setting is.
 */
@Composable
fun InkTheme(content: @Composable () -> Unit) {
    ApplyStatusBar(ink = true, dark = true)

    // Shared components read AppColors; Ink screens keep the cream palette they were built with.
    CompositionLocalProvider(LocalPaperPalette provides LightPaper) {
        MaterialTheme(
            colorScheme = InkColorScheme,
            typography = AppTypography,
            shapes = BinsooShapes,
            content = content
        )
    }
}
