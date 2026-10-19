package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotPaletteTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun empty_pixels_give_no_colours() {
        assertTrue(ShotPalette.dominantColors(IntArray(0)).isEmpty())
    }

    @Test
    fun zero_count_gives_no_colours() {
        assertTrue(ShotPalette.dominantColors(intArrayOf(argb(10, 20, 30)), count = 0).isEmpty())
    }

    @Test
    fun solid_image_gives_its_colour() {
        val pixels = IntArray(100) { argb(200, 120, 40) }
        assertEquals(listOf(argb(200, 120, 40)), ShotPalette.dominantColors(pixels))
    }

    @Test
    fun most_common_colour_comes_first() {
        val pixels = IntArray(10) { if (it < 7) argb(250, 0, 0) else argb(0, 0, 250) }
        val colors = ShotPalette.dominantColors(pixels, count = 2)
        assertEquals(listOf(argb(250, 0, 0), argb(0, 0, 250)), colors)
    }

    @Test
    fun near_identical_shades_are_merged() {
        // Two shades a few steps apart fall in the same bucket and are one colour.
        val pixels = IntArray(50) { if (it % 2 == 0) argb(100, 100, 100) else argb(102, 101, 100) }
        assertEquals(1, ShotPalette.dominantColors(pixels).size)
    }

    @Test
    fun count_limits_the_result() {
        val pixels = intArrayOf(
            argb(255, 0, 0), argb(0, 255, 0), argb(0, 0, 255),
            argb(255, 255, 0), argb(0, 255, 255), argb(255, 0, 255),
        )
        assertEquals(3, ShotPalette.dominantColors(pixels, count = 3).size)
    }

    @Test
    fun result_is_opaque() {
        val colors = ShotPalette.dominantColors(intArrayOf(argb(10, 20, 30), argb(200, 210, 220)))
        colors.forEach { assertEquals(0xFF, it ushr 24) }
    }
}
