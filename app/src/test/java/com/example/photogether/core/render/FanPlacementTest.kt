package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FanPlacementTest {
    private val chip = 36f
    private val margin = 10f
    private val topReserve = 34f

    private fun place(
        count: Int,
        ax: Float,
        ay: Float,
        w: Float = 800f,
        h: Float = 360f,
        mid: Float = 180f,
        radius: Float = 140f,
    ) = FanLayoutMath.place(
        count = count, anchorX = ax, anchorY = ay, radius = radius, ringGap = 46f, chipSize = chip,
        spacing = 40f, width = w, height = h, margin = margin, topReserve = topReserve, preferredMidDeg = mid,
    )

    private fun assertInside(p: FanLayoutMath.FanPlacement, w: Float, h: Float) {
        val half = chip / 2f
        p.points.forEach {
            assertTrue("x ${it.x}", it.x - half >= margin - 0.5f && it.x + half <= w - margin + 0.5f)
            assertTrue("y ${it.y}", it.y - half >= margin + topReserve - 0.5f && it.y + half <= h - margin + 0.5f)
        }
    }

    @Test
    fun buttonAtTopRightOfLandscapeWindowStillFitsEveryChip() {
        val p = place(count = 12, ax = 700f, ay = 45f)
        assertEquals(12, p.points.size)
        assertInside(p, 800f, 360f)
    }

    @Test
    fun buttonAtTheLeftEdgeKeepsTheFanOnScreen() {
        val p = place(count = 8, ax = 5f, ay = 180f, mid = 180f)
        assertInside(p, 800f, 360f)
    }

    @Test
    fun buttonAtTheRightEdgeKeepsTheFanOnScreen() {
        val p = place(count = 8, ax = 795f, ay = 180f, mid = 0f)
        assertInside(p, 800f, 360f)
    }

    @Test
    fun portraitButtonAtTheBottomOpensUpwardAndFits() {
        val p = place(count = 10, ax = 200f, ay = 700f, w = 400f, h = 800f, mid = 270f)
        assertInside(p, 400f, 800f)
        assertEquals(270f, p.midDeg, 0.01f)
        // Opens upward: every chip is above the button.
        assertTrue(p.points.all { it.y < 700f })
    }

    @Test
    fun fanThatFitsWhereItIsIsNotMoved() {
        val p = place(count = 6, ax = 400f, ay = 300f, w = 800f, h = 600f, mid = 270f)
        assertEquals(400f, p.centerX, 0.01f)
        assertEquals(300f, p.centerY, 0.01f)
        assertEquals(140f, p.radius, 0.01f)
    }

    @Test
    fun smallWindowShrinksTheRadius() {
        val p = place(count = 6, ax = 100f, ay = 100f, w = 200f, h = 200f, mid = 270f)
        assertTrue(p.radius < 140f)
        assertInside(p, 200f, 200f)
    }

    @Test
    fun manyEntriesUseMoreRingsAndKeepTheirSpacing() {
        val p = place(count = 14, ax = 400f, ay = 300f, w = 800f, h = 600f, mid = 270f)
        assertEquals(14, p.points.size)
        for (i in p.points.indices) for (j in i + 1 until p.points.size) {
            val d = kotlin.math.hypot(p.points[i].x - p.points[j].x, p.points[i].y - p.points[j].y)
            assertTrue("chips $i and $j are $d apart", d >= 30f)
        }
    }

    @Test
    fun noEntriesGivesNoPoints() {
        assertTrue(place(count = 0, ax = 100f, ay = 100f).points.isEmpty())
    }

    @Test
    fun shortCodeUsesInitialsOfSeveralWordsOrThreeLetters() {
        assertEquals("KOD", FanLayoutMath.shortCode("Kodachrome"))
        assertEquals("ORI", FanLayoutMath.shortCode("Original"))
        assertEquals("KG", FanLayoutMath.shortCode("Kodak Gold"))
        assertEquals("FCC", FanLayoutMath.shortCode("Fuji Classic-Chrome"))
        assertEquals("", FanLayoutMath.shortCode("  "))
    }
}
