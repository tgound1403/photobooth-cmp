package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewfinderSkinTest {
    private fun skin(id: String) = CameraProfiles.byId(id).recipe.skin

    @Test
    fun eachCameraHasItsSkin() {
        assertEquals(ViewfinderSkin.FILM_FRAME, skin("classic35"))
        assertEquals(ViewfinderSkin.RANGEFINDER, skin("fuji_mini"))
        assertEquals(ViewfinderSkin.SOFT_GLASS, skin("golden"))
        assertEquals(ViewfinderSkin.LCD_DIGICAM, skin("night"))
        assertEquals(ViewfinderSkin.RANGEFINDER_MONO, skin("silver"))
    }

    @Test
    fun shutterTintsAreDistinct() {
        val tints = CameraProfiles.ALL.map { it.recipe.skin.shutterTint }
        assertEquals(5, tints.toSet().size)
        assertTrue(tints.none { it == 0L })
    }
}
