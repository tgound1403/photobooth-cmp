package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ExposureMathTest {

    private val thirds = EvRange(minIndex = -3, maxIndex = 3, step = 1f / 3f)
    private val sixths = EvRange(minIndex = -6, maxIndex = 6, step = 1f / 6f)

    @Test
    fun indexForRoundsToNearestStep() {
        assertEquals(1, EvRange.SOFTWARE.indexFor(0.4f))
        assertEquals(3, EvRange.SOFTWARE.indexFor(0.9f))
        assertEquals(-3, EvRange.SOFTWARE.indexFor(-0.9f))
        assertEquals(0, EvRange.SOFTWARE.indexFor(0f))
    }

    @Test
    fun indexForClampsIntoRange() {
        assertEquals(6, EvRange.SOFTWARE.indexFor(100f))
        assertEquals(-6, EvRange.SOFTWARE.indexFor(-100f))
    }

    @Test
    fun evForClampsOutOfRangeIndex() {
        assertEquals(2f, EvRange.SOFTWARE.evFor(10), 1e-5f)
        assertEquals(-2f, EvRange.SOFTWARE.evFor(-10), 1e-5f)
    }

    @Test
    fun evForIndexForRoundTripsOnStep() {
        for (index in EvRange.SOFTWARE.minIndex..EvRange.SOFTWARE.maxIndex) {
            val ev = EvRange.SOFTWARE.evFor(index)
            assertEquals(index, EvRange.SOFTWARE.indexFor(ev))
        }
    }

    @Test
    fun minAndMaxEvMatchRange() {
        assertEquals(-2f, EvRange.SOFTWARE.minEv, 1e-5f)
        assertEquals(2f, EvRange.SOFTWARE.maxEv, 1e-5f)
    }

    @Test
    fun majorTickEveryFollowsStep() {
        assertEquals(3, thirds.majorTickEvery)
        assertEquals(6, sixths.majorTickEvery)
    }

    @Test
    fun softwareRangeIsPlusMinusTwoEv() {
        assertEquals(6, EvRange.SOFTWARE.maxIndex)
        assertEquals(-6, EvRange.SOFTWARE.minIndex)
        assertEquals(1f / 3f, EvRange.SOFTWARE.step, 1e-6f)
    }

    @Test
    fun formatEvShowsSignAndOneDecimal() {
        assertEquals("+0.3", formatEv(0.333f))
        assertEquals("+0.7", formatEv(0.7f))
        assertEquals("-1.3", formatEv(-1.3f))
        assertEquals("0.0", formatEv(0f))
    }

    @Test
    fun formatEvNeverShowsNegativeZero() {
        assertEquals("0.0", formatEv(-0.04f))
    }
}
