package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NdMathTest {
    @Test
    fun threeStopsIsAnEighth() {
        assertEquals(0.125f, NdMath.gain(NdMath.ndStops(1f)), 1e-6f)
        assertEquals(1f, NdMath.gain(NdMath.ndStops(0f)), 0f)
    }

    @Test
    fun gradedDensityFadesFromTopToBottom() {
        assertEquals(NdMath.MAX_GND_STOPS, NdMath.gradStopsAt(1f, 0f), 1e-6f)
        assertEquals(0f, NdMath.gradStopsAt(1f, 1f), 1e-6f)
        assertTrue(NdMath.gradStopsAt(1f, 0.3f) > NdMath.gradStopsAt(1f, 0.5f))
    }

    @Test
    fun ndCountsAsAnEditAndClamps() {
        assertTrue(!EditParams(nd = 0.1f).isIdentity)
        assertTrue(!EditParams(gnd = 0.1f).isIdentity)
        assertEquals(1f, EditParams(nd = 9f).clamped().nd, 0f)
    }
}
