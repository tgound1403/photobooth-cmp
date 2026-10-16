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

    @Test
    fun cameraFlashDefaults() {
        assertEquals(FlashMode.AUTO, CameraProfiles.byId("classic35").recipe.defaultFlash)
        assertEquals(FlashStyle.HARSH, CameraProfiles.byId("classic35").recipe.flashStyle)
        assertEquals(FlashMode.AUTO, CameraProfiles.byId("fuji_mini").recipe.defaultFlash)
        assertEquals(FlashStyle.WARM, CameraProfiles.byId("fuji_mini").recipe.flashStyle)
        assertEquals(FlashMode.OFF, CameraProfiles.byId("golden").recipe.defaultFlash)
        assertEquals(FlashStyle.WARM, CameraProfiles.byId("golden").recipe.flashStyle)
        assertEquals(FlashMode.ON, CameraProfiles.byId("night").recipe.defaultFlash)
        assertEquals(FlashStyle.HARSH, CameraProfiles.byId("night").recipe.flashStyle)
        assertEquals(FlashMode.OFF, CameraProfiles.byId("silver").recipe.defaultFlash)
        assertEquals(FlashStyle.NONE, CameraProfiles.byId("silver").recipe.flashStyle)
    }
}
