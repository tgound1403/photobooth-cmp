package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditParamsCropTest {

    @Test
    fun recipeWithoutCropOrChromaticStillDecodes() {
        // JSON written before crop and chromatic existed.
        val old = """{"filterId":"kodak","exposure":0.2,"vignette":0.4}"""
        val params = EditParams.fromJson(old)
        assertEquals("kodak", params.filterId)
        assertEquals(0.2f, params.exposure, 0f)
        assertEquals(CropParams(), params.crop)
        assertEquals(0f, params.chromatic, 0f)
    }

    @Test
    fun cropRoundTripsThroughJson() {
        val crop = CropParams(aspect = CropAspect.R4_5, quarterTurns = 1, flipHorizontal = true)
        val params = EditParams(crop = crop)
        val decoded = EditParams.fromJson(params.toJson())
        assertEquals(crop, decoded.crop)
        assertFalse(decoded.isIdentity)
    }

    @Test
    fun defaultCropIsIdentity() {
        assertTrue(EditParams().isIdentity)
        assertTrue(CropParams().isIdentity)
    }

    @Test
    fun clampNormalisesNegativeQuarterTurns() {
        val clamped = EditParams(crop = CropParams(quarterTurns = -1)).clamped()
        assertEquals(3, clamped.crop.quarterTurns)
    }
}
