package com.example.photogether.core.moments

/**
 * Colour choices for the home calendar sheet. Colours are ARGB ints, always stored opaque.
 * The text, grid and "weekend" colours are derived from the sheet colour so any pick stays readable.
 * Pure Kotlin, so it runs in JVM tests.
 */
object CalendarColors {

    val DEFAULT: Int = 0xFF211F1C.toInt()

    /**
     * Colours the app suggests: dark ones that make light prints stand out, plus two light papers.
     * The order matches the `calendar_color_names` string array.
     */
    val SUGGESTED: List<Int> = listOf(
        0xFF211F1C.toInt(), // charcoal
        0xFF1B2A41.toInt(), // midnight blue
        0xFF1E3A2F.toInt(), // forest
        0xFF4A1D2B.toInt(), // burgundy
        0xFF3A2A4D.toInt(), // plum
        0xFF8A4B34.toInt(), // terracotta
        0xFFFFFEF8.toInt(), // cream paper
        0xFFC8D5B9.toInt(), // sage
    )

    /** Forces full opacity: a see-through sheet would show the page through the prints. */
    fun opaque(argb: Int): Int = argb or 0xFF000000.toInt()

    /** Perceived brightness, 0 (black) to 1 (white). */
    fun luminance(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    fun isLight(argb: Int): Boolean = luminance(argb) > 0.55f

    /** Day numbers and other main text on the sheet. */
    fun textOn(argb: Int): Int = if (isLight(argb)) 0xFF1B1A17.toInt() else 0xFFF1F0E6.toInt()

    /** Weekday names and the day numbers of empty days. */
    fun dimTextOn(argb: Int): Int = if (isLight(argb)) 0xFF6E6D63.toInt() else 0xFFB4B3A8.toInt()

    /** Saturday and Sunday. */
    fun weekendOn(argb: Int): Int = if (isLight(argb)) 0xFFD9342B.toInt() else 0xFFFF7A70.toInt()

    /** Thin lines between the day cells. */
    fun gridLineOn(argb: Int): Int = if (isLight(argb)) 0x1F000000 else 0x17FFFFFF

    /** "#RRGGBB" text for [argb]. */
    fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** Colour from "#RRGGBB" or "RRGGBB"; null when the text is not a 6-digit hex colour. */
    fun parseHex(text: String): Int? {
        val digits = text.trim().removePrefix("#")
        if (digits.length != 6) return null
        val rgb = digits.toIntOrNull(16) ?: return null
        return opaque(rgb)
    }
}
