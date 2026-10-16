package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomMathTest {
    @Test
    fun thresholdIsZeroBelowKneeAndOneAtWhite() {
        assertEquals(0f, BloomMath.thresholdWeight(0.5f), 0f)
        assertEquals(0f, BloomMath.thresholdWeight(0.7f), 1e-6f)
        assertEquals(1f, BloomMath.thresholdWeight(1.0f), 1e-6f)
        assertEquals(1f, BloomMath.thresholdWeight(1.5f), 0f)
    }

    @Test
    fun thresholdIsMonotonicAndBounded() {
        var prev = -1f
        for (i in -10..120) {
            val w = BloomMath.thresholdWeight(i / 100f)
            assertTrue("weight $w out of range", w in 0f..1f)
            assertTrue("not monotonic at ${i / 100f}", w >= prev)
            prev = w
        }
    }

    @Test
    fun gaussianWeightsAreNormalisedSymmetricAndPeaked() {
        val w = BloomMath.gaussianWeights()
        assertEquals(9, w.size)
        assertEquals(1.0, w.sumOf { it.toDouble() }, 1e-5)
        for (i in 0 until 4) assertEquals(w[i], w[8 - i], 1e-7f)
        assertEquals(w.max(), w[4], 0f)
        assertTrue(w.indices.all { it == 4 || w[it] < w[4] })
    }

    @Test
    fun bloomScaleClampsToUnitRange() {
        assertEquals(0f, BloomMath.bloomScale(0f), 0f)
        assertEquals(0f, BloomMath.bloomScale(-1f), 0f)
        assertEquals(1f, BloomMath.bloomScale(2f), 0f)
        assertEquals(0.5f, BloomMath.bloomScale(0.5f), 0f)
    }
}
