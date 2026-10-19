package com.example.photogether.core.camera

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

    @Test
    fun lensOptionsAreRealLensesSortedAndDistinct() {
        val physical = listOf(Lens("tele", 70f), Lens("main", 26.2f), Lens("main2", 26.4f), Lens("ultrawide", 16f))
        assertEquals(listOf(16f, 26f, 70f), lensOptions(physical, 26f, 4f))
    }

    @Test
    fun lensOptionsFallBackToDigitalStopsForSingleLens() {
        val single = listOf(Lens("0", 26f))
        assertEquals(listOf(26f, 39f, 52f), lensOptions(single, 26f, 2f))
    }

    @Test
    fun lensOptionsFallBackLimitedByMaxZoom() {
        assertEquals(listOf(26f, 39f), lensOptions(emptyList(), 26f, 1.6f))
    }

    @Test
    fun closestOptionPicksNearestMm() {
        assertEquals(39f, closestOption(listOf(26f, 39f, 52f), 45f), 0f)
        assertEquals(26f, closestOption(listOf(26f, 39f, 52f), 20f), 0f)
    }

    @Test
    fun closestOptionWithoutOptionsReturnsCurrent() {
        assertEquals(35f, closestOption(emptyList(), 35f), 0f)
    }

    @Test
    fun ratioLabelDropsTrailingZeroAndKeepsOneDecimal() {
        assertEquals("1x", ratioLabel(26f, 26f))
        assertEquals("1.5x", ratioLabel(39f, 26f))
        assertEquals("2x", ratioLabel(52f, 26f))
        assertEquals("0.6x", ratioLabel(16f, 26f))
    }
}
