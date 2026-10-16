package com.example.cameraxapp.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LensSelectionTest {

    private val lenses = listOf(
        Lens("ultrawide", 16f),
        Lens("main", 26f),
        Lens("tele", 70f),
    )

    @Test
    fun fullFrameSensorKeepsFocalLength() {
        assertEquals(50f, equivalentFocalMm(50f, 36f, 24f), 0.1f)
    }

    @Test
    fun smallerSensorMultipliesFocalLength() {
        // A 6.3 x 4.7 mm sensor has a diagonal of about 7.86mm (crop about 5.5), so 5.3mm is about 29mm.
        assertEquals(29f, equivalentFocalMm(5.3f, 6.3f, 4.7f), 0.5f)
    }

    @Test
    fun picksLongestLensNotAboveTarget() {
        assertEquals("main", pickLens(35f, lenses)?.cameraId)
        assertEquals("tele", pickLens(75f, lenses)?.cameraId)
        assertEquals("ultrawide", pickLens(20f, lenses)?.cameraId)
    }

    @Test
    fun fallsBackToShortestLensWhenAllAreLonger() {
        assertEquals("ultrawide", pickLens(10f, lenses)?.cameraId)
    }

    @Test
    fun noLensGivesNull() {
        assertNull(pickLens(35f, emptyList()))
    }

    @Test
    fun zoomIsTargetOverLensFocal() {
        assertEquals(35f / 26f, zoomFor(35f, lenses[1]), 0.001f)
    }
}
