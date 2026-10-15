package com.example.cameraxapp.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LutMathTest {

    private val eps = 1e-6f

    @Test
    fun lutUv_originIsFirstTexelCentre() {
        val (u, v) = lutUv(0, 0, 0)
        assertEquals(0.5f / 512f, u, eps)
        assertEquals(0.5f / 512f, v, eps)
    }

    @Test
    fun lutUv_maxInputIsLastTexelCentre() {
        val (u, v) = lutUv(63, 63, 63)
        // Blue 63 -> column 7, row 7: texel (7 * 64 + 63, 7 * 64 + 63) centre at +0.5.
        assertEquals(511.5f / 512f, u, eps)
        assertEquals(511.5f / 512f, v, eps)
    }

    @Test
    fun lutUv_blueSelectsTileColumnAndRow() {
        // b = 8 -> column 0, row 1
        val (u1, v1) = lutUv(5, 9, 8)
        assertEquals((0 * 64 + 5 + 0.5f) / 512f, u1, eps)
        assertEquals((1 * 64 + 9 + 0.5f) / 512f, v1, eps)

        // b = 15 -> column 7, row 1
        val (u2, v2) = lutUv(0, 0, 15)
        assertEquals((7 * 64 + 0.5f) / 512f, u2, eps)
        assertEquals((1 * 64 + 0.5f) / 512f, v2, eps)
    }

    @Test
    fun lutUv_staysInsideItsTile() {
        for (b in 0 until 64) {
            val (u, v) = lutUv(0, 0, b)
            val col = b % 8
            val row = b / 8
            assertTrue(u * 512f >= col * 64f && u * 512f <= col * 64f + 64f)
            assertTrue(v * 512f >= row * 64f && v * 512f <= row * 64f + 64f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun lutUv_rejectsOutOfRange() {
        lutUv(64, 0, 0)
    }

    @Test
    fun exposureGain_mapsMinusOneToPlusOneToQuarterToFourTimes() {
        assertEquals(1f, exposureGain(0f), eps)
        assertEquals(4f, exposureGain(1f), eps)
        assertEquals(0.25f, exposureGain(-1f), eps)
        assertEquals(2f, exposureGain(0.5f), eps)
    }

    @Test
    fun lumaWeights_areRec709AndSumToOne() {
        assertEquals(0.2126f, LUMA_R, eps)
        assertEquals(0.7152f, LUMA_G, eps)
        assertEquals(0.0722f, LUMA_B, eps)
        assertEquals(1f, LUMA_R + LUMA_G + LUMA_B, eps)
        assertEquals(1f, luma(1f, 1f, 1f), eps)
        assertEquals(0.2126f, luma(1f, 0f, 0f), eps)
        assertEquals(0.7152f, luma(0f, 1f, 0f), eps)
        assertEquals(0.0722f, luma(0f, 0f, 1f), eps)
    }

    @Test
    fun scaledSize_scalesLongestSideAndKeepsAspect() {
        assertEquals(200 to 100, scaledSize(1000, 500, 200))
        assertEquals(100 to 200, scaledSize(500, 1000, 200))
    }

    @Test
    fun scaledSize_neverUpscalesAndNullKeepsSize() {
        assertEquals(100 to 50, scaledSize(100, 50, 200))
        assertEquals(100 to 50, scaledSize(100, 50, null))
    }

    @Test
    fun scaledSize_keepsAtLeastOnePixel() {
        assertEquals(1000 to 1, scaledSize(3000, 1, 1000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun scaledSize_rejectsNonPositiveMax() {
        scaledSize(100, 50, 0)
    }

    @Test
    fun whiteBalanceGains_neutralAtZero() {
        assertEquals(Triple(1f, 1f, 1f), whiteBalanceGains(0f, 0f))
    }

    @Test
    fun whiteBalanceGains_temperatureAndTint() {
        val warm = whiteBalanceGains(1f, 0f)
        assertEquals(1.1f, warm.first, eps)
        assertEquals(1f, warm.second, eps)
        assertEquals(0.9f, warm.third, eps)

        val magenta = whiteBalanceGains(0f, 1f)
        assertEquals(1.05f, magenta.first, eps)
        assertEquals(0.9f, magenta.second, eps)
        assertEquals(1.05f, magenta.third, eps)
    }

    @Test
    fun contrastFactor_isOneHalfToOneAndAHalf() {
        assertEquals(1f, contrastFactor(0f), eps)
        assertEquals(0.5f, contrastFactor(-1f), eps)
        assertEquals(1.5f, contrastFactor(1f), eps)
    }

    @Test
    fun saturationFactor_zeroIsGrayscale() {
        assertEquals(0f, saturationFactor(-1f), eps)
        assertEquals(1f, saturationFactor(0f), eps)
        assertEquals(2f, saturationFactor(1f), eps)
    }

    @Test
    fun strengths_matchDocumentedScales() {
        assertEquals(0.1f, fadeAmount(1f), eps)
        assertEquals(0.06f, grainAmplitude(1f), eps)
        assertEquals(0.6f, vignetteStrength(1f), eps)
        assertEquals(0f, vignetteStrength(0f), eps)
    }

    @Test
    fun toneAmplitude_keepsHighlightShadowMappingMonotone() {
        // The offset slope is bounded by 3 * amplitude because each smoothstep weight changes at most 3x per unit luma.
        assertTrue(3f * TONE_AMPLITUDE < 1f)
    }
}
