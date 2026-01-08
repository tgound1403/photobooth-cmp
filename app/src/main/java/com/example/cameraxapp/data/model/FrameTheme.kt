package com.example.cameraxapp.data.model

import android.graphics.Color

data class FrameTheme(
        val id: String,
        val name: String,
        val backgroundColor: Int,
        val accentColor: Int? = null,
        val pattern: ThemePattern = ThemePattern.SOLID
)

enum class ThemePattern {
    SOLID,
    GRADIENT,
    DOTS,
    STRIPES
}

object FrameThemes {
    val CLASSIC_WHITE =
            FrameTheme(id = "classic_white", name = "Classic White", backgroundColor = Color.WHITE)

    val RETRO_BEIGE =
            FrameTheme(
                    id = "retro_beige",
                    name = "Retro Beige",
                    backgroundColor = Color.parseColor("#F5E6D3")
            )

    val SUNSET_GRADIENT =
            FrameTheme(
                    id = "sunset",
                    name = "Sunset",
                    backgroundColor = Color.parseColor("#FF6B6B"),
                    accentColor = Color.parseColor("#FFD93D"),
                    pattern = ThemePattern.GRADIENT
            )

    val OCEAN_GRADIENT =
            FrameTheme(
                    id = "ocean",
                    name = "Ocean",
                    backgroundColor = Color.parseColor("#4ECDC4"),
                    accentColor = Color.parseColor("#1A535C"),
                    pattern = ThemePattern.GRADIENT
            )

    val PASTEL_PINK =
            FrameTheme(
                    id = "pastel_pink",
                    name = "Pastel Pink",
                    backgroundColor = Color.parseColor("#FFB6C1"),
                    accentColor = Color.parseColor("#FFC0CB"),
                    pattern = ThemePattern.DOTS
            )

    val NEON_PURPLE =
            FrameTheme(
                    id = "neon_purple",
                    name = "Neon Purple",
                    backgroundColor = Color.parseColor("#9D4EDD"),
                    accentColor = Color.parseColor("#C77DFF"),
                    pattern = ThemePattern.GRADIENT
            )

    val MINT_GREEN =
            FrameTheme(
                    id = "mint",
                    name = "Mint Green",
                    backgroundColor = Color.parseColor("#98FF98")
            )

    val DARK_VINTAGE =
            FrameTheme(
                    id = "dark_vintage",
                    name = "Dark Vintage",
                    backgroundColor = Color.parseColor("#2C2C2C"),
                    accentColor = Color.parseColor("#404040"),
                    pattern = ThemePattern.STRIPES
            )

    val ALL_THEMES =
            listOf(
                    CLASSIC_WHITE,
                    RETRO_BEIGE,
                    SUNSET_GRADIENT,
                    OCEAN_GRADIENT,
                    PASTEL_PINK,
                    NEON_PURPLE,
                    MINT_GREEN,
                    DARK_VINTAGE
            )
}
