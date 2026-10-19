package com.example.photogether.core.colors

import com.example.photogether.core.render.EditParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookDetailsTest {

    @Test
    fun neutralLookHasNoLines() {
        assertTrue(LookDetails.lines(EditParams()).isEmpty())
    }

    @Test
    fun calmPhotoListsBlendAndSaturation() {
        val lines = LookDetails.lines(
            EditParams(filterIntensity = 0.75f, saturation = -0.25f),
        )
        assertEquals(
            listOf("Cường độ bộ lọc: 75%", "Độ bão hòa: -25%"),
            lines,
        )
    }

    @Test
    fun exposureIsShownInEvStops() {
        val lines = LookDetails.lines(EditParams(exposure = 0.36f, highlight = -0.3f))
        assertEquals(listOf("Phơi sáng: +0.72 EV", "Vùng sáng: -30%"), lines)
    }
}
