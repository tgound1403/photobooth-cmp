package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAspectTest {
    @Test fun portraitRatios() {
        assertEquals(0.75f, CameraAspect.FOUR_THREE.widthOverHeight, 1e-6f)
        assertEquals(1f, CameraAspect.ONE_ONE.widthOverHeight, 1e-6f)
        assertEquals(2f / 3f, CameraAspect.THREE_TWO.widthOverHeight, 1e-6f)
        assertEquals(9f / 16f, CameraAspect.SIXTEEN_NINE.widthOverHeight, 1e-6f)
    }

    @Test fun onlyFourThreeKeepsTheFullFrame() {
        assertFalse(CameraAspect.FOUR_THREE.crops)
        assertTrue(CameraAspect.ONE_ONE.crops)
    }

    @Test fun squareCropOfLandscapeTrimsSides() {
        assertEquals(PixelRect(500, 0, 3500, 3000), CropMath.centerCrop(4000, 3000, 1f))
    }

    @Test fun sidewaysSourceUsesInverseRatio() {
        // Landscape 4000x3000 source shown rotated; displayed 2:3 portrait means 3:2 landscape, trimmed top and bottom.
        val r = CropMath.centerCrop(4000, 3000, 1f / (2f / 3f))
        assertEquals(PixelRect(0, 166, 4000, 2833), r)
    }

    @Test fun portraitImageTrimsTopAndBottom() {
        assertEquals(PixelRect(0, 500, 3000, 3500), CropMath.centerCrop(3000, 4000, 1f))
    }

    @Test fun fourThreeMatchesPortraitSensorWithNoTrim() {
        assertEquals(PixelRect(0, 0, 3000, 4000), CropMath.centerCrop(3000, 4000, CameraAspect.FOUR_THREE.widthOverHeight))
    }
}
