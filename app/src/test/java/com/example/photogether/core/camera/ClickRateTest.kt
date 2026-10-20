package com.example.photogether.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickRateTest {
    @Test
    fun wheelIsLowerPitchedThanSlider() {
        assertTrue(Click.WHEEL.rate < Click.SLIDER.rate)
        assertEquals(0.7f, Click.WHEEL.rate, 0.001f)
        assertEquals(1f, Click.SLIDER.rate, 0.001f)
    }

    @Test
    fun jitterStaysCloseToTheBaseRate() {
        Click.entries.forEach { click ->
            val lo = click.playbackRate(-1f)
            val hi = click.playbackRate(1f)
            assertTrue(lo < hi)
            assertEquals(click.rate, click.playbackRate(0f), 0.0001f)
            assertTrue(lo >= click.rate * 0.9f && hi <= click.rate * 1.1f)
        }
    }

    @Test
    fun wheelWithJitterIsStillBelowSliderWithJitter() {
        assertTrue(Click.WHEEL.playbackRate(1f) < Click.SLIDER.playbackRate(-1f))
    }
}
