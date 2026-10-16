package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Test

class MomentSizeTest {

    @Test
    fun orientation_0_keeps_size() {
        assertEquals(1280 to 960, displaySize(1280, 960, 0))
    }

    @Test
    fun orientation_90_swaps_size() {
        assertEquals(960 to 1280, displaySize(1280, 960, 90))
    }

    @Test
    fun orientation_180_keeps_size() {
        assertEquals(1280 to 960, displaySize(1280, 960, 180))
    }

    @Test
    fun orientation_270_swaps_size() {
        assertEquals(960 to 1280, displaySize(1280, 960, 270))
    }

    @Test
    fun zero_dimension_is_carried_over_not_invented() {
        assertEquals(960 to 0, displaySize(0, 960, 90))
        assertEquals(0 to 1280, displaySize(1280, 0, 270))
        assertEquals(0 to 0, displaySize(0, 0, 90))
    }
}
