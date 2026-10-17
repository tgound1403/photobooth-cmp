package com.example.photogether.core.render

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

    @Test
    fun visibleCropOnExactFrameViewMatchesCenterCrop() {
        val r = CropMath.visibleCrop(300f, 400f, 3f / 4f, 1f)
        assertEquals(0f, r.left, 0.01f); assertEquals(300f, r.right, 0.01f)
        assertEquals(50f, r.top, 0.01f); assertEquals(350f, r.bottom, 0.01f)
    }

    @Test
    fun visibleCropOnNarrowViewFollowsFrameNotView() {
        // View 300x500 (narrower than 3:4): frame is scaled to height 500, width 375, sides cut by the view.
        // The 1:1 saved crop is 375 wide, so it spans the whole view width, not a view-width square.
        val r = CropMath.visibleCrop(300f, 500f, 3f / 4f, 1f)
        assertEquals(0f, r.left, 0.01f); assertEquals(300f, r.right, 0.01f)
        assertEquals(375f, r.height, 0.01f); assertEquals(62.5f, r.top, 0.01f)
    }

    @Test
    fun visibleCropOnWideViewKeepsSquareOfFrameWidth() {
        // View 400x400 (wider than 3:4): frame 400x533, 1:1 crop is 400x400 = the whole view.
        val r = CropMath.visibleCrop(400f, 400f, 3f / 4f, 1f)
        assertEquals(400f, r.width, 0.01f); assertEquals(400f, r.height, 0.01f)
    }

    @Test
    fun zoomCropFloatShrinksAboutCentre() {
        val r = CropMath.zoomCrop(FloatRect(0f, 100f, 300f, 500f), 2f)
        assertEquals(75f, r.left, 0.01f); assertEquals(225f, r.right, 0.01f)
        assertEquals(200f, r.top, 0.01f); assertEquals(400f, r.bottom, 0.01f)
    }

    @Test
    fun zoomCropFactorOneOrLessKeepsRect() {
        val f = FloatRect(10f, 20f, 110f, 220f)
        assertEquals(f, CropMath.zoomCrop(f, 1f))
        assertEquals(f, CropMath.zoomCrop(f, 0.5f))
        val p = PixelRect(5, 6, 105, 206)
        assertEquals(p, CropMath.zoomCrop(p, 1f))
    }

    @Test
    fun zoomCropPixelIsCentredAndKeepsRatio() {
        val base = CropMath.centerCrop(3000, 4000, 3f / 4f)
        val z = CropMath.zoomCrop(base, 2f)
        assertEquals(PixelRect(750, 1000, 2250, 3000), z)
    }

    @Test
    fun zoomCropPixelNeverEmptyOrOutside() {
        val z = CropMath.zoomCrop(PixelRect(0, 0, 10, 10), 1000f)
        assertEquals(1, z.width); assertEquals(1, z.height)
        assert(z.left >= 0 && z.top >= 0 && z.right <= 10 && z.bottom <= 10)
    }

    @Test
    fun unclippedCropOverflowsViewAndVisibleCropClipsIt() {
        // 400x300 view: the 3:4 frame fills the width (400x533) and overflows the height.
        val u = CropMath.unclippedCrop(400f, 300f, 3f / 4f, 3f / 4f)
        assertEquals(-116.67f, u.top, 0.01f); assertEquals(416.67f, u.bottom, 0.01f)
        val v = CropMath.visibleCrop(400f, 300f, 3f / 4f, 3f / 4f)
        assertEquals(0f, v.top, 0.01f); assertEquals(300f, v.bottom, 0.01f)
        // Zooming the unclipped rect 2x gives a frame inside the view, unlike zooming the clipped one.
        val z = CropMath.clipTo(CropMath.zoomCrop(u, 2f), 400f, 300f)
        assertEquals(16.67f, z.top, 0.01f); assertEquals(283.33f, z.bottom, 0.01f)
    }
}
