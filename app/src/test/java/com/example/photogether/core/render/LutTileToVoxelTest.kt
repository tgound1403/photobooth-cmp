package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class LutTileToVoxelTest {
    @Test
    fun origin() = assertEquals(LutTexel(0, 0), lutTileToVoxel(0, 0, 0))

    @Test
    fun farCorner() = assertEquals(LutTexel(511, 511), lutTileToVoxel(63, 63, 63))

    @Test
    fun blueNineIsSecondRowSecondColumn() = assertEquals(LutTexel(64, 64), lutTileToVoxel(0, 0, 9))

    @Test
    fun blueSevenIsLastColumnFirstRow() = assertEquals(LutTexel(448, 0), lutTileToVoxel(0, 0, 7))

    @Test
    fun pixelIndexMatchesTexelForEveryVoxel() {
        for (b in 0 until LUT_LEVELS) for (g in 0 until LUT_LEVELS) for (r in 0 until LUT_LEVELS) {
            val t = lutTileToVoxel(r, g, b)
            assertEquals(t.y * LUT_SIZE + t.x, lutVoxelPixelIndex(r, g, b))
        }
    }
}
