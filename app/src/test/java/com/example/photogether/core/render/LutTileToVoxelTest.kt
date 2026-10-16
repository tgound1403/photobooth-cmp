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
}
