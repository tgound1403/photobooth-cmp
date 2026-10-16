package com.example.photogether.core.render

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DateStampMathTest {
    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance(utc).apply { clear(); set(y, m - 1, d, 12, 0, 0) }.timeInMillis

    @Test fun digitsAreYearMonthDay() {
        assertArrayEquals(intArrayOf(2, 6, 1, 0, 1, 6), DateStampMath.digitsOf(millis(2026, 10, 16), utc))
    }

    @Test fun singleDigitMonthAndDayArePadded() {
        assertArrayEquals(intArrayOf(0, 5, 0, 3, 0, 9), DateStampMath.digitsOf(millis(2005, 3, 9), utc))
    }

    @Test fun maskOfEightIsAllSegments() {
        assertEquals(0x7F, DateStampMath.glyphMask(8))
    }

    @Test fun maskOfOneUsesTwoSegments() {
        assertEquals(2, Integer.bitCount(DateStampMath.glyphMask(1)))
    }

    @Test fun outOfRangeDigitHasNoSegments() {
        assertEquals(0, DateStampMath.glyphMask(-1))
        assertEquals(0, DateStampMath.glyphMask(10))
    }
}
