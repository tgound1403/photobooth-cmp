package com.example.photogether.core.render

import java.util.Calendar
import java.util.TimeZone

/** Pure maths for the camera date stamp: the digits to print and the 7-segment glyph masks. */
object DateStampMath {
    const val DIGIT_COUNT = 6

    /**
     * Segment bits, standard layout: a top, b top right, c bottom right, d bottom, e bottom left,
     * f top left, g middle. Bit 0 is a, bit 6 is g. Index = digit 0..9.
     */
    val GLYPH_MASKS: IntArray = intArrayOf(0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F)

    /** Segment bitmask of [digit] (0..9); 0 for anything else. */
    fun glyphMask(digit: Int): Int = if (digit in 0..9) GLYPH_MASKS[digit] else 0

    /** Six digits in the order Y Y M M D D (year mod 100) for [epochMillis] in [zone]. */
    fun digitsOf(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): IntArray =
        IntArray(DIGIT_COUNT).also { fillDigits(epochMillis, zone, it) }

    /** Same as [digitsOf], writing into [out] (size at least [DIGIT_COUNT]). */
    fun fillDigits(epochMillis: Long, zone: TimeZone, out: IntArray) {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = epochMillis
        val yy = cal.get(Calendar.YEAR) % 100
        val mm = cal.get(Calendar.MONTH) + 1
        val dd = cal.get(Calendar.DAY_OF_MONTH)
        out[0] = yy / 10; out[1] = yy % 10
        out[2] = mm / 10; out[3] = mm % 10
        out[4] = dd / 10; out[5] = dd % 10
    }

    /** Width of the six digits and separators at scale 1, in pixels (matches the shader layout). */
    const val BASE_WIDTH = 174f

    /** Stamp scale for a [width] x [height] display: [sizeScale], shrunk so the stamp spans at most half the width. */
    fun stampScale(width: Int, height: Int): Float = minOf(sizeScale(height), width * 0.5f / BASE_WIDTH)

    /** Stamp scale: output height / 1080, clamped to 0.5..4 (same rule as grain). */
    fun sizeScale(outputHeight: Int): Float = (outputHeight / 1080f).coerceIn(0.5f, 4f)
}
