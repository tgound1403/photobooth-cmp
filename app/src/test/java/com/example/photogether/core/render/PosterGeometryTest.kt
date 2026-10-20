package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterGeometryTest {
    @Test
    fun photoSitsBelowTitleBandAndFrameWrapsIt() {
        val l = PosterGeometry.compute(1000, 1500)
        assertEquals(1080f, l.frameWidth, 0.01f)
        assertEquals(420f + 1500f + 100f, l.frameHeight, 0.01f)
        assertEquals(1000f, l.photoDest.width, 0.01f)
        assertEquals(1500f, l.photoDest.height, 0.01f)
        assertEquals(420f, l.photoDest.top, 0.01f)
    }

    @Test
    fun coverSourceKeepsAspectOfWindowAndStaysInside() {
        val s = PosterGeometry.coverSource(1000, 1500, 1000f, 1700f)
        assertEquals(1000f / 1700f, s.width / s.height, 0.001f)
        assertTrue(s.left >= 0f && s.top >= 0f && s.right <= 1000f && s.bottom <= 1500f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPhoto() {
        PosterGeometry.compute(0, 10)
    }
}
