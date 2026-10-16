package com.example.photogether.core.render

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectMathTest {

    @Test
    fun zeroChromaticStrengthIsIdentity() {
        assertEquals(0f, EffectMath.chromaticShiftPx(0f), 0f)
        assertEquals(0f, EffectMath.chromaticShiftPx(-1f), 0f)
    }

    @Test
    fun chromaticShiftScalesAndIsClamped() {
        assertEquals(20f, EffectMath.chromaticShiftPx(1f), 0f)
        assertEquals(6f, EffectMath.chromaticShiftPx(0.3f), 1e-5f)
        assertEquals(40f, EffectMath.chromaticShiftPx(99f), 0f)
    }

    @Test
    fun specWithoutEffectsIsNone() {
        assertTrue(EffectSpec.of(0f, null, null, null).isNone)
        assertTrue(EffectSpec.NONE.isNone)
        assertTrue(EffectSpec.of(0f, null, "bogus", null).isNone)
    }

    @Test
    fun distortionPresetsAddToChromatic() {
        val spec = EffectSpec.of(0.5f, null, "Distortion 5", null)
        assertEquals(1.5f, spec.chromatic, 1e-6f)
        assertEquals(2f, EffectSpec.of(1f, null, "Distortion 5", null).chromatic, 1e-6f)
        assertEquals(0.5f, EffectSpec.of(0f, null, "Distortion 3", null).grain, 1e-6f)
        assertEquals(0.7f, EffectSpec.of(0f, null, "Distortion 4", null).vignette, 1e-6f)
    }

    @Test
    fun leakGradientIsDeterministicWithCornersAndPositiveRadius() {
        OverlayCatalog.lightLeaks.forEach { name ->
            val g = EffectMath.leakGradient(name)
            assertEquals(g, EffectMath.leakGradient(name))
            assertTrue(g.cornerX == 0f || g.cornerX == 1f)
            assertTrue(g.cornerY == 0f || g.cornerY == 1f)
            assertTrue("radius ${g.radiusFactor}", g.radiusFactor > 0f && g.radiusFactor <= 1.0f)
        }
    }

    @Test
    fun leakColourIsInRangeAndFadesOut() {
        for (i in 0..120) {
            val rgba = EffectMath.leakRgba(i / 100f)
            rgba.forEach { assertTrue("component $it at step $i", it in 0f..1f) }
        }
        assertEquals(150f / 255f, EffectMath.leakRgba(0f)[3], 1e-6f)
        assertEquals(100f / 255f, EffectMath.leakRgba(0.5f)[3], 1e-6f)
        assertEquals(0f, EffectMath.leakRgba(1f)[3], 0f)
        assertEquals(0f, EffectMath.leakRgba(5f)[3], 0f)
    }

    @Test
    fun vignetteIsZeroInsideAndBoundedByIntensity() {
        assertEquals(0f, EffectMath.vignetteAlpha(0.5f, 0f), 0f)
        assertEquals(0f, EffectMath.vignetteAlpha(0f, 0.7f), 0f)
        assertEquals(0f, EffectMath.vignetteAlpha(0.3f, 0.7f), 0f)
        assertEquals(0.7f, EffectMath.vignetteAlpha(1f, 0.7f), 1e-6f)
        assertEquals(0.7f, EffectMath.vignetteAlpha(3f, 0.7f), 1e-6f)
        var last = 0f
        for (i in 0..100) {
            val a = EffectMath.vignetteAlpha(i / 50f, 0.7f)
            assertTrue(a in 0f..0.7f)
            assertTrue(a >= last)
            last = a
        }
    }

    @Test
    fun scratchLinesAreDeterministicAndCounted() {
        OverlayCatalog.scratches.forEach { name ->
            val a = EffectMath.scratchLines(name, 1000, 800)
            assertArrayEquals(a, EffectMath.scratchLines(name, 1000, 800), 0f)
            val count = a.size / 4
            assertTrue("count $count", count in 20..EffectMath.SCRATCH_MAX_LINES)
            for (i in 0 until count) {
                // Start points lie inside the image; ends are within 50 px of the start.
                assertTrue(a[i * 4] in 0f..1000f)
                assertTrue(a[i * 4 + 1] in 0f..800f)
                assertTrue(kotlin.math.abs(a[i * 4 + 2] - a[i * 4]) <= 50f)
                assertTrue(kotlin.math.abs(a[i * 4 + 3] - a[i * 4 + 1]) <= 50f)
            }
        }
    }
}
