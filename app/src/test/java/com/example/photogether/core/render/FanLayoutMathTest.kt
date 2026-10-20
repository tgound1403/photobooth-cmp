package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FanLayoutMathTest {
    private val eps = 0.01f

    @Test
    fun singleEntryIsStraightAboveCentre() {
        val p = FanLayoutMath.entryPosition(0, 1, 100f, 200f, 150f)
        assertEquals(100f, p.x, eps)
        assertEquals(50f, p.y, eps)
    }

    @Test
    fun fiveEntriesAreSymmetricAboutTheVerticalAxis() {
        val pts = List(5) { FanLayoutMath.entryPosition(it, 5, 0f, 0f, 150f) }
        for (i in 0 until 5) {
            val a = pts[i]
            val b = pts[4 - i]
            assertEquals(-a.x, b.x, eps)
            assertEquals(a.y, b.y, eps)
        }
        assertEquals(0f, pts[2].x, eps)
        assertEquals(-150f, pts[2].y, eps)
        assertTrue(pts[0].x < 0f && pts[4].x > 0f)
    }

    @Test
    fun layoutSplitsAcrossTwoArcsAboveNine() {
        assertEquals(9, FanLayoutMath.layout(9, 0f, 0f, 150f, 50f).size)
        val pts = FanLayoutMath.layout(10, 0f, 0f, 150f, 50f)
        assertEquals(10, pts.size)
        val radii = pts.map { kotlin.math.hypot(it.x, it.y) }
        assertEquals(5, radii.count { kotlin.math.abs(it - 100f) < eps })
        assertEquals(5, radii.count { kotlin.math.abs(it - 150f) < eps })
        assertTrue(FanLayoutMath.layout(0, 0f, 0f, 150f, 50f).isEmpty())
    }

    @Test
    fun nearestIndexPicksClosestWithinRadius() {
        val pts = listOf(FanPoint(0f, 0f), FanPoint(100f, 0f))
        assertEquals(0, FanLayoutMath.nearestIndex(pts, 10f, 5f, 48f))
        assertEquals(1, FanLayoutMath.nearestIndex(pts, 60f, 0f, 48f))
    }

    @Test
    fun nearestIndexIsNullBeyondHitRadius() {
        val pts = listOf(FanPoint(0f, 0f), FanPoint(100f, 0f))
        assertNull(FanLayoutMath.nearestIndex(pts, 50f, 100f, 48f))
        assertNull(FanLayoutMath.nearestIndex(emptyList(), 0f, 0f, 48f))
    }
}
