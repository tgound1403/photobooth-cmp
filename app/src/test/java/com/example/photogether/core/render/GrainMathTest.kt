package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrainMathTest {
    @Test fun weightEndpointsAndPeak() {
        assertEquals(0f, GrainMath.luminanceWeight(0f), 1e-6f)
        assertEquals(0f, GrainMath.luminanceWeight(1f), 1e-6f)
        assertEquals(1f, GrainMath.luminanceWeight(0.5f), 1e-6f)
    }

    @Test fun weightInRangeAndSymmetric() {
        for (i in 0..100) {
            val l = i / 100f
            val w = GrainMath.luminanceWeight(l)
            assertTrue(w in 0f..1f)
            assertEquals(w, GrainMath.luminanceWeight(1f - l), 1e-5f)
        }
        assertEquals(0f, GrainMath.luminanceWeight(-0.5f), 0f)
        assertEquals(0f, GrainMath.luminanceWeight(1.5f), 0f)
    }

    @Test fun sizeScale() {
        assertEquals(1f, GrainMath.sizeScale(1080), 1e-6f)
        assertEquals(0.5f, GrainMath.sizeScale(540), 1e-6f)
        assertEquals(0.5f, GrainMath.sizeScale(100), 1e-6f)
        assertEquals(2f, GrainMath.sizeScale(2160), 1e-6f)
        assertEquals(4f, GrainMath.sizeScale(4320), 1e-6f)
        assertEquals(4f, GrainMath.sizeScale(10000), 1e-6f)
    }
}
