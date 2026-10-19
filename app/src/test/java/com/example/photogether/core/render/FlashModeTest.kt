package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashModeTest {

    @Test
    fun flashPlanMapsModes() {
        assertEquals(FlashPlan(0, false), flashPlan(FlashMode.OFF, 0, 1, 2))
        assertEquals(FlashPlan(1, false), flashPlan(FlashMode.AUTO, 0, 1, 2))
        assertEquals(FlashPlan(2, false), flashPlan(FlashMode.ON, 0, 1, 2))
        assertEquals(FlashPlan(0, true), flashPlan(FlashMode.TORCH, 0, 1, 2))
    }

    @Test
    fun flashModeCyclesAndWraps() {
        assertEquals(FlashMode.AUTO, FlashMode.OFF.next())
        assertEquals(FlashMode.OFF, FlashMode.TORCH.next())
    }
}
