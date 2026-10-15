package com.example.cameraxapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

private val PhotogetherColorScheme = darkColorScheme(
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

private val PhotogetherShapes = Shapes(
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg)
)

/** The one app theme. Dark only. */
@Composable
fun PhotogetherTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = PhotogetherColorScheme,
        typography = AppTypography,
        shapes = PhotogetherShapes,
        content = content
    )
}
