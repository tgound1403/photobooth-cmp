package com.example.photogether.ui.onboarding.anim

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimMathTest {
    private val eps = 1e-5f

    @Test fun segmentClampsToWindow() {
        assertEquals(0f, AnimMath.segment(0.1f, 0.2f, 0.6f), eps)
        assertEquals(0.5f, AnimMath.segment(0.4f, 0.2f, 0.6f), eps)
        assertEquals(1f, AnimMath.segment(0.9f, 0.2f, 0.6f), eps)
    }

    @Test fun easingEndpoints() {
        assertEquals(0f, AnimMath.easeInOut(0f), eps)
        assertEquals(1f, AnimMath.easeInOut(1f), eps)
        assertEquals(0f, AnimMath.easeOutCubic(0f), eps)
        assertEquals(1f, AnimMath.easeOutCubic(1f), eps)
    }

    @Test fun pulsePeaksInMiddleOfWindow() {
        assertEquals(0f, AnimMath.pulse(0.2f, 0.2f, 0.6f), eps)
        assertEquals(1f, AnimMath.pulse(0.4f, 0.2f, 0.6f), eps)
        assertEquals(0f, AnimMath.pulse(0.6f, 0.2f, 0.6f), eps)
    }

    @Test fun lerpInterpolates() {
        assertEquals(15f, AnimMath.lerp(10f, 20f, 0.5f), eps)
    }
}
