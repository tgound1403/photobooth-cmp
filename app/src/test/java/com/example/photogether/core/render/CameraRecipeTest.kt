package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraRecipeTest {

    @Test
    fun everyProfileRecipeIsAlreadyClamped() {
        CameraProfiles.ALL.forEach { profile ->
            assertEquals(profile.id, profile.recipe, profile.recipe.clamped())
        }
    }

    @Test
    fun classic35HasOrangeDateStamp() {
        assertEquals(DateStampStyle.ORANGE_LCD, CameraProfiles.byId("classic35").recipe.dateStamp)
    }

    @Test
    fun silverIsSquare() {
        assertEquals(CameraAspect.ONE_ONE, CameraProfiles.byId("silver").recipe.aspect)
    }

    @Test
    fun clampedLimitsFloatsToUnitRange() {
        val clamped = CameraRecipe(leak = 2f, dust = -1f, halation = 2f, grain = -1f, vignette = 2f).clamped()
        assertEquals(1f, clamped.leak, 0f)
        assertEquals(0f, clamped.dust, 0f)
        assertEquals(1f, clamped.halation, 0f)
        assertEquals(0f, clamped.grain, 0f)
        assertEquals(1f, clamped.vignette, 0f)
    }
}
