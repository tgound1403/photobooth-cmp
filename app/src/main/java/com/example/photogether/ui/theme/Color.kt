package com.example.photogether.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One set of paper colours. [LightPaper] is the cream default; [DarkPaper] is used when the
 * dark theme setting is on. Every paper value is read through [AppColors].
 */
class PaperPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val highlight: Color,
    val danger: Color,
)

/** Cream background, black ink. */
val LightPaper = PaperPalette(
    background = Color(0xFFE9E8DC),
    surface = Color(0xFFF1F0E6),
    surfaceElevated = Color(0xFFDAD9CC),
    outline = Color(0xFF0B0B0B),
    textPrimary = Color(0xFF0B0B0B),
    textSecondary = Color(0xFF6E6D63),
    accent = Color(0xFF0B0B0B),
    onAccent = Color(0xFFE9E8DC),
    highlight = Color(0xFFF2C21B),
    danger = Color(0xFFD9342B),
)

/** Warm near-black background, cream ink. Same roles as [LightPaper]. */
val DarkPaper = PaperPalette(
    background = Color(0xFF121210),
    surface = Color(0xFF1A1A17),
    surfaceElevated = Color(0xFF2A2A26),
    outline = Color(0xFFE9E8DC),
    textPrimary = Color(0xFFE9E8DC),
    textSecondary = Color(0xFF9C9B92),
    accent = Color(0xFFE9E8DC),
    onAccent = Color(0xFF121210),
    highlight = Color(0xFFF2C21B),
    danger = Color(0xFFFF6B61),
)

/** The palette in effect. Set by [PhotogetherTheme]; [InkTheme] resets it to [LightPaper]. */
val LocalPaperPalette = staticCompositionLocalOf { LightPaper }

/**
 * The paper colour set. Each value follows the palette of the enclosing theme, so screens
 * read these inside composition and never write Color(0x...).
 */
object AppColors {
    val background: Color @Composable get() = LocalPaperPalette.current.background
    val surface: Color @Composable get() = LocalPaperPalette.current.surface
    val surfaceElevated: Color @Composable get() = LocalPaperPalette.current.surfaceElevated
    val outline: Color @Composable get() = LocalPaperPalette.current.outline
    val textPrimary: Color @Composable get() = LocalPaperPalette.current.textPrimary
    val textSecondary: Color @Composable get() = LocalPaperPalette.current.textSecondary

    /** Primary button fill. Solid ink. */
    val accent: Color @Composable get() = LocalPaperPalette.current.accent
    val onAccent: Color @Composable get() = LocalPaperPalette.current.onAccent

    /** Yellow: focus ring, tag text, selected underline. */
    val highlight: Color @Composable get() = LocalPaperPalette.current.highlight
    val danger: Color @Composable get() = LocalPaperPalette.current.danger
}

/**
 * The ink colour set (black background, white ink). Used by the camera, the camera picker and the
 * photo booth capture and edit screens. These screens stay dark in both themes.
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
