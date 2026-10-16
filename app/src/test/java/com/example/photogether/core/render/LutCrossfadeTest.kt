package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LutCrossfadeTest {
    private val d = 180_000_000L

    @Test fun finishedBeforeStart() {
        assertEquals(1f, LutCrossfade(d).mixAt(5L), 0f)
    }

    @Test fun zeroAtStartOneAfterDuration() {
        val c = LutCrossfade(d)
        c.start(1_000L)
        assertEquals(0f, c.mixAt(1_000L), 0f)
        assertEquals(0.5f, c.mixAt(1_000L + d / 2), 1e-6f)
        assertEquals(1f, c.mixAt(1_000L + d), 0f)
        assertEquals(1f, c.mixAt(1_000L + 10 * d), 0f)
    }

    @Test fun monotonic() {
        val c = LutCrossfade(d)
        c.start(0L)
        var prev = -1f
        for (i in 0..200) {
            val m = c.mixAt(i * d / 100)
            assertTrue(m >= prev)
            prev = m
        }
    }

    @Test fun restartResetsTimer() {
        val c = LutCrossfade(d)
        c.start(0L)
        assertTrue(c.mixAt(d / 2) > 0.4f)
        c.start(d / 2)
        assertEquals(0f, c.mixAt(d / 2), 0f)
        assertTrue(c.mixAt(d / 2 + d / 4) < 1f)
        assertEquals(1f, c.mixAt(d / 2 + d), 0f)
    }
}
