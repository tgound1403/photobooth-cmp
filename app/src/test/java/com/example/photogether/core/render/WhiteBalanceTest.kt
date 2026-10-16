package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WhiteBalanceTest {

    private val eps = 1e-4f

    private fun lumaOf(r: Float, g: Float, b: Float): Float =
        LUMA_R * r + LUMA_G * g + LUMA_B * b

    @Test
    fun kelvinGains_neutralIsUnity() {
        val (r, g, b) = kelvinGains(5500)
        assertEquals(1f, r, eps)
        assertEquals(1f, g, eps)
        assertEquals(1f, b, eps)
    }

    @Test
    fun kelvinGains_nullIsUnity() {
        assertEquals(Triple(1f, 1f, 1f), kelvinGains(null))
    }

    @Test
    fun kelvinGains_lumaIsOneAcrossRange() {
        for (k in listOf(2000, 3200, 4500, 6500, 8000, 10000)) {
            val (r, g, b) = kelvinGains(k)
            assertEquals("luma at ${k}K", 1f, lumaOf(r, g, b), eps)
        }
    }

    @Test
    fun kelvinGains_redRisesAndBlueFallsWithKelvin() {
        // Below about 2680 K the [0.5, 2.0] clamp plus luma normalisation makes the blue gain
        // non-monotonic, so the sample starts at 2800 K.
        val samples = listOf(2800, 3200, 4500, 5500, 6500, 8000, 10000)
        val gains = samples.map { kelvinGains(it) }
        for (i in 0 until samples.size - 1) {
            val (r0, _, b0) = gains[i]
            val (r1, _, b1) = gains[i + 1]
            assertTrue("gR must rise: ${samples[i]}K -> ${samples[i + 1]}K", r1 > r0)
            assertTrue("gB must fall: ${samples[i]}K -> ${samples[i + 1]}K", b1 < b0)
        }
    }

    @Test
    fun kelvinGains_allChannelsWithinBounds() {
        for (k in Kelvin.MIN..Kelvin.MAX step Kelvin.SNAP) {
            val (r, g, b) = kelvinGains(k)
            for (c in listOf(r, g, b)) {
                assertTrue("gain $c out of [0.4, 2.5] at ${k}K", c in 0.4f..2.5f)
            }
        }
    }

    @Test
    fun sliderToKelvin_endpoints() {
        assertEquals(2000, sliderToKelvin(0f))
        assertEquals(10000, sliderToKelvin(1f))
    }

    @Test
    fun kelvinToSlider_roundTripWithinOneSnapStep() {
        for (k in Kelvin.MIN..Kelvin.MAX step Kelvin.SNAP) {
            val back = sliderToKelvin(kelvinToSlider(k))
            assertTrue(
                "round trip ${k}K -> $back",
                abs(back - k) <= Kelvin.SNAP,
            )
        }
    }

    @Test
    fun snapKelvin_clampsAndRounds() {
        assertEquals(2000, snapKelvin(1500f))
        assertEquals(5500, snapKelvin(5523f))
    }

    @Test
    fun planckianXy_matchesD65Region() {
        val (x, y) = planckianXy(6500f)
        assertEquals(0.313f, x, 0.003f)
        assertEquals(0.324f, y, 0.003f)
    }
}
