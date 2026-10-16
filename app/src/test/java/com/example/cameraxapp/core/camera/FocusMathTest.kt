package com.example.cameraxapp.core.camera

import com.example.cameraxapp.core.render.FloatRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusMathTest {

    private val eps = 1e-4f

    private fun assertPair(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, eps)
        assertEquals(expected.second, actual.second, eps)
    }

    private fun assertRect(expected: FloatRect, actual: FloatRect) {
        assertEquals(expected.left, actual.left, eps)
        assertEquals(expected.top, actual.top, eps)
        assertEquals(expected.right, actual.right, eps)
        assertEquals(expected.bottom, actual.bottom, eps)
    }

    // normalize

    @Test
    fun normalizeMapsInsidePointToFraction() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, 100f, 100f, 200f))
    }

    @Test
    fun normalizeClampsNegativeXAndOversizedYToRange() {
        assertPair(0f to 1f, FocusMath.normalize(-10f, 300f, 100f, 200f))
    }

    @Test
    fun normalizeClampsOversizedXAndNegativeYToRange() {
        assertPair(1f to 0f, FocusMath.normalize(150f, -1f, 100f, 200f))
    }

    @Test
    fun normalizeReturnsCentreWhenWidthIsZero() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, 100f, 0f, 200f))
    }

    @Test
    fun normalizeReturnsCentreWhenHeightIsZero() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, 100f, 100f, 0f))
    }

    @Test
    fun normalizeReturnsCentreWhenSizeIsNegative() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, 100f, -100f, -200f))
    }

    @Test
    fun normalizeReturnsCentreWhenWidthIsNaN() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, 100f, Float.NaN, 200f))
    }

    @Test
    fun normalizeReturnsCentreWhenCoordinateIsNaN() {
        assertPair(0.5f to 0.5f, FocusMath.normalize(Float.NaN, 100f, 100f, 200f))
        assertPair(0.5f to 0.5f, FocusMath.normalize(50f, Float.NaN, 100f, 200f))
    }

    @Test
    fun normalizeAlwaysStaysInUnitRange() {
        val coords = listOf(-1000f, -1f, 0f, 49f, 100f, 101f, 5000f)
        for (x in coords) {
            for (y in coords) {
                val (nx, ny) = FocusMath.normalize(x, y, 100f, 200f)
                assertTrue("x=$x -> $nx", nx in 0f..1f)
                assertTrue("y=$y -> $ny", ny in 0f..1f)
            }
        }
    }

    // indicatorRect

    @Test
    fun indicatorRectAtTopLeftCornerIsPulledInside() {
        assertRect(FloatRect(0f, 0f, 72f, 72f), FocusMath.indicatorRect(0f, 0f, 72f, 1080f, 1920f))
    }

    @Test
    fun indicatorRectAtBottomRightCornerIsPulledInside() {
        assertRect(
            FloatRect(1008f, 1848f, 1080f, 1920f),
            FocusMath.indicatorRect(1080f, 1920f, 72f, 1080f, 1920f),
        )
    }

    @Test
    fun indicatorRectAtCentreIsCentred() {
        val rect = FocusMath.indicatorRect(540f, 960f, 72f, 1080f, 1920f)
        assertEquals(540f, (rect.left + rect.right) / 2f, eps)
        assertEquals(960f, (rect.top + rect.bottom) / 2f, eps)
        assertEquals(72f, rect.width, eps)
        assertEquals(72f, rect.height, eps)
    }

    @Test
    fun indicatorRectLargerThanFrameIsCentredOnBothAxes() {
        val rect = FocusMath.indicatorRect(10f, 90f, 200f, 100f, 100f)
        assertRect(FloatRect(-50f, -50f, 150f, 150f), rect)
        assertEquals(50f, (rect.left + rect.right) / 2f, eps)
        assertEquals(50f, (rect.top + rect.bottom) / 2f, eps)
    }

    @Test
    fun indicatorRectStaysInsideFrameForManyPoints() {
        val xs = listOf(-500f, 0f, 36f, 540f, 1044f, 1080f, 3000f)
        val ys = listOf(-500f, 0f, 36f, 960f, 1884f, 1920f, 3000f)
        for (x in xs) {
            for (y in ys) {
                val rect = FocusMath.indicatorRect(x, y, 72f, 1080f, 1920f)
                assertTrue("rect $rect for ($x,$y)", rect.left >= -eps && rect.top >= -eps)
                assertTrue("rect $rect for ($x,$y)", rect.right <= 1080f + eps && rect.bottom <= 1920f + eps)
                assertEquals(72f, rect.width, eps)
                assertEquals(72f, rect.height, eps)
            }
        }
    }
}
