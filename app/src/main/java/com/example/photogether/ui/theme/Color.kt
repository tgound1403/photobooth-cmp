package com.example.photogether.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The paper colour set (cream background, black ink). Used by Home, Gallery, Share sheet,
 * Settings and Editor. Screens read colours from here (or from MaterialTheme.colorScheme,
 * which is built from these values) and never write Color(0x...).
 */
object AppColors {
    val background = Color(0xFFE9E8DC)
    val surface = Color(0xFFF1F0E6)
    val surfaceElevated = Color(0xFFDAD9CC)
    val outline = Color(0xFF0B0B0B)
    val textPrimary = Color(0xFF0B0B0B)
    val textSecondary = Color(0xFF6E6D63)

    /** Primary button fill. Solid black ink. */
    val accent = Color(0xFF0B0B0B)
    val onAccent = Color(0xFFE9E8DC)

    /** Yellow: focus ring, tag text, selected underline. */
    val highlight = Color(0xFFF2C21B)
    val danger = Color(0xFFD9342B)
}

/**
 * The ink colour set (black background, white ink). Used by the camera and the camera picker.
 * Same property names as [AppColors].
 */
object InkColors {
    val background = Color(0xFF000000)
    val surface = Color(0xFF111111)

    /** Translucent dark pills: apply alpha 0.7 at the call site. */
    val surfaceElevated = Color(0xFF26262A)
    val outline = Color.White.copy(alpha = 0.35f)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFA8A8A0)

    /** Cream shutter pill and the "A" button. */
    val accent = Color(0xFFE9E8DC)
    val onAccent = Color(0xFF0B0B0B)
    val highlight = Color(0xFFF2C21B)

    /** Not in the spec. Reuses the paper danger red so the property exists on both sets. */
    val danger = Color(0xFFD9342B)
}

// ---------------------------------------------------------------------------------------------
// Deprecated aliases. Kept only so screens that are rewritten in a later phase
// (PhotoBoothScreen, CameraScreen, EditImageScreen) still compile. Remove them with those phases.
// ---------------------------------------------------------------------------------------------

@Deprecated("Use AppColors.accent", ReplaceWith("AppColors.accent", "com.example.photogether.ui.theme.AppColors"))
val NeonCyan = AppColors.accent

@Deprecated("Use AppColors.accent", ReplaceWith("AppColors.accent", "com.example.photogether.ui.theme.AppColors"))
val NeonPurple = AppColors.accent

@Deprecated("Use AppColors.background", ReplaceWith("AppColors.background", "com.example.photogether.ui.theme.AppColors"))
val DeepBlack = AppColors.background

@Deprecated("Use AppColors.surface", ReplaceWith("AppColors.surface", "com.example.photogether.ui.theme.AppColors"))
val SurfaceBlack = AppColors.surface

@Deprecated("Use AppColors.textPrimary", ReplaceWith("AppColors.textPrimary", "com.example.photogether.ui.theme.AppColors"))
val TextWhite = AppColors.textPrimary
