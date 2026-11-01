package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarColorsTest {

    @Test
    fun opaque_forcesFullAlpha() {
        assertEquals(0xFF123456.toInt(), CalendarColors.opaque(0x00123456))
        assertEquals(0xFF123456.toInt(), CalendarColors.opaque(0x80123456.toInt()))
    }

    @Test
    fun darkAndLightSheetsGetOppositeText() {
        val dark = 0xFF211F1C.toInt()
        val light = 0xFFFFFEF8.toInt()
        assertFalse(CalendarColors.isLight(dark))
        assertTrue(CalendarColors.isLight(light))
        assertTrue(CalendarColors.luminance(CalendarColors.textOn(dark)) > 0.8f)
        assertTrue(CalendarColors.luminance(CalendarColors.textOn(light)) < 0.2f)
    }

    @Test
    fun textStaysReadableOnEverySuggestedColour() {
        for (color in CalendarColors.SUGGESTED) {
            val gap = kotlin.math.abs(CalendarColors.luminance(color) - CalendarColors.luminance(CalendarColors.textOn(color)))
            assertTrue("text on ${CalendarColors.hex(color)} too faint ($gap)", gap > 0.4f)
        }
    }

    @Test
    fun suggestedColoursAreOpaqueAndDistinct() {
        assertEquals(CalendarColors.SUGGESTED.size, CalendarColors.SUGGESTED.toSet().size)
        assertTrue(CalendarColors.SUGGESTED.all { it ushr 24 == 0xFF })
        assertTrue(CalendarColors.DEFAULT in CalendarColors.SUGGESTED)
    }

    @Test
    fun hexRoundTrips() {
        assertEquals("#1B2A41", CalendarColors.hex(0xFF1B2A41.toInt()))
        assertEquals(0xFF1B2A41.toInt(), CalendarColors.parseHex("#1b2a41"))
        assertEquals(0xFF1B2A41.toInt(), CalendarColors.parseHex(" 1B2A41 "))
    }

    @Test
    fun parseHexRejectsBadInput() {
        assertNull(CalendarColors.parseHex(""))
        assertNull(CalendarColors.parseHex("#12345"))
        assertNull(CalendarColors.parseHex("#GGGGGG"))
        assertNull(CalendarColors.parseHex("#1234567"))
    }
}
