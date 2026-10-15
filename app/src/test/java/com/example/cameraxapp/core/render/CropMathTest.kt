package com.example.cameraxapp.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {

    @Test
    fun rotatedSizeSwapsOnOddQuarterTurns() {
        assertEquals(3000 to 4000, CropMath.rotatedSize(3000, 4000, 0))
        assertEquals(3000 to 4000, CropMath.rotatedSize(3000, 4000, 2))
        assertEquals(4000 to 3000, CropMath.rotatedSize(3000, 4000, 1))
        assertEquals(4000 to 3000, CropMath.rotatedSize(3000, 4000, -1))
        assertEquals(4000 to 3000, CropMath.rotatedSize(3000, 4000, 5))
    }

    @Test
    fun freeCropKeepsWholeImage() {
        assertEquals(PixelRect(0, 0, 1600, 900), CropMath.cropRect(1600, 900, CropAspect.FREE))
    }

    @Test
    fun fourByFiveOnLandscapeTrimsSides() {
        // 900 px tall, 720 px wide, centred horizontally.
        assertEquals(PixelRect(440, 0, 1160, 900), CropMath.cropRect(1600, 900, CropAspect.R4_5))
    }

    @Test
    fun squareOnPortraitTrimsTopAndBottom() {
        assertEquals(PixelRect(0, 350, 900, 1250), CropMath.cropRect(900, 1600, CropAspect.SQUARE))
    }

    @Test
    fun nineBySixteenOnLandscape() {
        assertEquals(PixelRect(656, 0, 1264, 1080), CropMath.cropRect(1920, 1080, CropAspect.R9_16))
    }

    @Test
    fun everyFixedAspectStaysInsideTheImage() {
        val sizes = listOf(1000 to 1500, 1600 to 900, 1200 to 1200)
        for ((w, h) in sizes) {
            for (aspect in CropAspect.values()) {
                val r = CropMath.cropRect(w, h, aspect)
                assert(r.left >= 0 && r.top >= 0 && r.right <= w && r.bottom <= h) { "$aspect $w x $h -> $r" }
                aspect.ratio?.let { target ->
                    val actual = r.width.toFloat() / r.height
                    assert(kotlin.math.abs(actual - target) < 0.01f) { "$aspect $w x $h ratio $actual" }
                }
            }
        }
    }
}
