package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LutLayoutTest {
    @Test
    fun picksLayoutFromBitmapSize() {
        assertEquals(64, LutLayout.fromSize(512, 512)?.levels)
        assertEquals(32, LutLayout.fromSize(256, 128)?.levels)
    }

    @Test
    fun rejectsUnsupportedSizes() {
        assertNull(LutLayout.fromSize(256, 256))
        assertNull(LutLayout.fromSize(128, 128))
        assertNull(LutLayout.fromSize(512, 256))
    }

    @Test
    fun levels32OriginAndCornerAreFirstAndLastTexel() {
        val l = LutLayout.LEVELS_32
        assertEquals(0, l.voxelPixelIndex(0, 0, 0))
        // Last blue slice is tile (7, 3): x = 7 * 32 + 31 = 255, y = 3 * 32 + 31 = 127.
        assertEquals(127 * 256 + 255, l.voxelPixelIndex(31, 31, 31))
    }

    @Test
    fun levels32BlueSelectsTileColumnAndRow() {
        val l = LutLayout.LEVELS_32
        // Blue 9 is tile (1, 1): x = 32 + r, y = 32 + g.
        assertEquals(33 * 256 + 37, l.voxelPixelIndex(5, 1, 9))
    }

    @Test
    fun levels32CoversEveryTexelExactlyOnce() {
        val l = LutLayout.LEVELS_32
        val seen = BooleanArray(l.width * l.height)
        for (b in 0 until 32) for (g in 0 until 32) for (r in 0 until 32) {
            val i = l.voxelPixelIndex(r, g, b)
            assertTrue("index $i out of range", i in seen.indices)
            assertEquals("index $i used twice", false, seen[i])
            seen[i] = true
        }
        assertTrue(seen.all { it })
    }

    @Test
    fun levels64MatchesLegacyPixelIndex() {
        val l = LutLayout.LEVELS_64
        for (b in 0 until 64) for (g in 0 until 64) for (r in 0 until 64) {
            assertEquals(lutVoxelPixelIndex(r, g, b), l.voxelPixelIndex(r, g, b))
        }
    }
}
