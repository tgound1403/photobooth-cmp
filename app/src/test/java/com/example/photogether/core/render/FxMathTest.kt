package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FxMathTest {
    private val seeds = (-500..500).map { it * 7919 } + listOf(0, 1, Int.MAX_VALUE, Int.MIN_VALUE)

    @Test
    fun offsetsAreInUnitRange() {
        for (s in seeds) {
            for ((x, y) in listOf(FxMath.leakOffset(s), FxMath.dustOffset(s))) {
                assertTrue("x=$x seed=$s", x >= 0f && x < 1f)
                assertTrue("y=$y seed=$s", y >= 0f && y < 1f)
            }
        }
    }

    @Test
    fun sameSeedSameResult() {
        assertEquals(FxMath.leakOffset(42), FxMath.leakOffset(42))
        assertEquals(FxMath.dustOffset(42), FxMath.dustOffset(42))
        assertEquals(FxMath.leakFlip(42), FxMath.leakFlip(42))
    }

    @Test
    fun leakAndDustOffsetsDiffer() {
        for (s in listOf(0, 1, 1234, 99999)) {
            assertNotEquals("seed=$s", FxMath.leakOffset(s), FxMath.dustOffset(s))
        }
    }

    @Test
    fun scaleClamps() {
        assertEquals(0f, FxMath.leakScale(0f), 0f)
        assertEquals(0f, FxMath.leakScale(-1f), 0f)
        assertEquals(1f, FxMath.leakScale(2f), 0f)
        assertEquals(0.5f, FxMath.leakScale(0.5f), 0f)
        assertEquals(0f, FxMath.dustScale(0f), 0f)
        assertEquals(0f, FxMath.dustScale(-1f), 0f)
        assertEquals(1f, FxMath.dustScale(2f), 0f)
        assertEquals(0.5f, FxMath.dustScale(0.5f), 0f)
    }

    @Test
    fun captureSeedIsNonNegativeAndVaries() {
        for (n in listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 123_456_789_012L)) {
            assertTrue("nowNs=$n", FxMath.newCaptureSeed(n) >= 0)
        }
        assertNotEquals(FxMath.newCaptureSeed(1_000_000L), FxMath.newCaptureSeed(1_000_001L))
        assertNotEquals(FxMath.newCaptureSeed(0L), FxMath.newCaptureSeed(1L))
    }
}
