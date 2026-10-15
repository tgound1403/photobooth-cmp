package com.example.cameraxapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The single dark colour set of the app. Screens read colours from here (or from
 * MaterialTheme.colorScheme, which is built from these values) and never write Color(0x...).
 */
object AppColors {
    val background = Color(0xFF0B0B0D)
    val surface = Color(0xFF16161A)
    val surfaceElevated = Color(0xFF1F1F25)
    val outline = Color(0xFF2A2A31)
    val textPrimary = Color(0xFFF2F2F5)
    val textSecondary = Color(0xFF9A9AA5)

    /** Warm amber. The only accent colour, used sparingly. */
    val accent = Color(0xFFFFB547)
    val onAccent = Color(0xFF1A1205)
    val danger = Color(0xFFFF5A5F)
}

// ---------------------------------------------------------------------------------------------
// Deprecated aliases. Kept only so screens that are rewritten in a later phase
// (PhotoBoothScreen, CameraScreen, EditImageScreen) still compile. Remove them with those phases.
// ---------------------------------------------------------------------------------------------

@Deprecated("Use AppColors.accent", ReplaceWith("AppColors.accent", "com.example.cameraxapp.ui.theme.AppColors"))
val NeonCyan = AppColors.accent

@Deprecated("Use AppColors.accent", ReplaceWith("AppColors.accent", "com.example.cameraxapp.ui.theme.AppColors"))
val NeonPurple = AppColors.accent

@Deprecated("Use AppColors.background", ReplaceWith("AppColors.background", "com.example.cameraxapp.ui.theme.AppColors"))
val DeepBlack = AppColors.background

@Deprecated("Use AppColors.surface", ReplaceWith("AppColors.surface", "com.example.cameraxapp.ui.theme.AppColors"))
val SurfaceBlack = AppColors.surface

@Deprecated("Use AppColors.textPrimary", ReplaceWith("AppColors.textPrimary", "com.example.cameraxapp.ui.theme.AppColors"))
val TextWhite = AppColors.textPrimary
