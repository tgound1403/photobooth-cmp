package com.example.cameraxapp.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PolaroidStyleTest {

    private val eps = 0.01f

    private val sizes = listOf(
        "portrait" to (1000 to 1500),
        "landscape" to (1600 to 900),
        "square" to (1200 to 1200),
    )

    private fun assertRect(name: String, expected: FloatRect, actual: FloatRect) {
        assertEquals("$name left", expected.left, actual.left, eps)
        assertEquals("$name top", expected.top, actual.top, eps)
        assertEquals("$name right", expected.right, actual.right, eps)
        assertEquals("$name bottom", expected.bottom, actual.bottom, eps)
    }

    private fun inside(inner: FloatRect, w: Int, h: Int): Boolean =
        inner.left >= -eps && inner.top >= -eps && inner.right <= w + eps && inner.bottom <= h + eps

    @Test
    fun classicFitKeepsOriginalNumbers() {
        val layout = PolaroidGeometry.compute(4000, 3000, PolaroidMode.FIT, PolaroidStyles.CLASSIC)
        assertEquals(4480f, layout.frameWidth, eps)
        assertEquals(5280f, layout.frameHeight, eps)
        assertRect("area", FloatRect(240f, 240f, 4240f, 4240f), layout.photoArea)
        assertRect("dest", FloatRect(240f, 740f, 4240f, 3740f), layout.photoDest)
        assertRect("source", FloatRect(0f, 0f, 4000f, 3000f), layout.sourceRect)
        assertRect("caption", FloatRect(0f, 4240f, 4480f, 5280f), layout.captionBox)
    }

    @Test
    fun classicFillDefaultPlacementKeepsOldCrop() {
        val layout = PolaroidGeometry.compute(4000, 3000, PolaroidMode.FILL)
        assertRect("source", FloatRect(500f, 0f, 3500f, 3000f), layout.sourceRect)
        assertRect("dest", layout.photoArea, layout.photoDest)
    }

    @Test
    fun titleTopFitGeometry() {
        val layout = PolaroidGeometry.compute(4000, 3000, PolaroidMode.FIT, PolaroidStyles.TITLE_TOP)
        assertEquals(4480f, layout.frameWidth, eps)
        assertEquals(5200f, layout.frameHeight, eps)
        assertRect("area", FloatRect(240f, 880f, 4240f, 4880f), layout.photoArea)
        assertRect("caption", FloatRect(0f, 0f, 4480f, 880f), layout.captionBox)
    }

    @Test
    fun fillWithZoomAndOffsetMovesTheCrop() {
        val placement = PolaroidPlacement(zoom = 2f, offsetX = 1f, offsetY = -1f)
        val layout = PolaroidGeometry.compute(4000, 3000, PolaroidMode.FILL, PolaroidStyles.CLASSIC, placement)
        assertRect("source", FloatRect(2500f, 0f, 4000f, 1500f), layout.sourceRect)
        assertRect("dest", layout.photoArea, layout.photoDest)
    }

    @Test
    fun fillStaysInsidePhotoAtEdgesOfZoomAndOffset() {
        val zooms = listOf(0.5f, 1f, 4f, 10f, Float.NaN)
        val offsets = listOf(-5f, -1f, 0f, 1f, 5f, Float.NaN)
        for ((name, size) in sizes) {
            val (w, h) = size
            for (zoom in zooms) {
                for (ox in offsets) {
                    for (oy in offsets) {
                        val placement = PolaroidPlacement(zoom, ox, oy)
                        val src = PolaroidGeometry.compute(w, h, PolaroidMode.FILL, PolaroidStyles.CLASSIC, placement).sourceRect
                        val label = "$name zoom=$zoom ox=$ox oy=$oy"
                        assertTrue("$label inside photo", inside(src, w, h))
                        assertEquals("$label square", src.width, src.height, 0.001f)
                    }
                }
            }
        }
    }

    @Test
    fun fitIgnoresPlacement() {
        val placement = PolaroidPlacement(zoom = 2f, offsetX = 0.5f, offsetY = -0.5f)
        for ((name, size) in sizes) {
            val (w, h) = size
            val plain = PolaroidGeometry.compute(w, h, PolaroidMode.FIT)
            val moved = PolaroidGeometry.compute(w, h, PolaroidMode.FIT, PolaroidStyles.CLASSIC, placement)
            assertEquals("$name fit with placement", plain, moved)
        }
    }

    @Test
    fun placementClampedPullsValuesIntoRange() {
        assertEquals(PolaroidPlacement(1f, 0f, 0f), PolaroidPlacement(0.5f, Float.NaN, 0f).clamped())
        assertEquals(PolaroidPlacement(4f, 1f, -1f), PolaroidPlacement(10f, 5f, -5f).clamped())
        assertEquals(PolaroidPlacement(1f, 0f, 0f), PolaroidPlacement(Float.NaN, Float.NaN, Float.NaN).clamped())
    }

    @Test
    fun textColorOnPicksReadableContrast() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        assertEquals(black, FrameColors.textColorOn(0xFFFFFFFF.toInt()))
        assertEquals(black, FrameColors.textColorOn(0xFFF6F0E1.toInt()))
        assertEquals(black, FrameColors.textColorOn(0xFFE8DCC4.toInt()))
        assertEquals(black, FrameColors.textColorOn(0xFFDCE8F2.toInt()))
        assertEquals(white, FrameColors.textColorOn(0xFF5B6B4A.toInt()))
        assertEquals(white, FrameColors.textColorOn(0xFF3A3A3C.toInt()))
        assertEquals(white, FrameColors.textColorOn(0xFF111111.toInt()))
    }

    @Test
    fun paletteHasSevenColoursInOrder() {
        assertEquals(7, FrameColors.PALETTE.size)
        assertEquals(0xFFFFFFFF.toInt(), FrameColors.PALETTE.first())
        assertEquals(0xFF111111.toInt(), FrameColors.PALETTE.last())
    }

    @Test
    fun byIdFallsBackToClassic() {
        assertEquals(PolaroidStyles.CLASSIC, PolaroidStyles.byId(null))
        assertEquals(PolaroidStyles.CLASSIC, PolaroidStyles.byId(""))
        assertEquals(PolaroidStyles.CLASSIC, PolaroidStyles.byId("xyz"))
    }

    @Test
    fun byIdFindsEveryStyle() {
        assertEquals(4, PolaroidStyles.ALL.size)
        assertEquals(PolaroidStyles.ALL.size, PolaroidStyles.ALL.map { it.id }.toSet().size)
        for (style in PolaroidStyles.ALL) {
            assertEquals(style, PolaroidStyles.byId(style.id))
        }
    }

    @Test
    fun frameSpecDefaultsKeepClassicWhiteFrame() {
        val frame = FrameSpec()
        assertEquals("classic", frame.polaroidStyleId)
        assertEquals(0xFFFFFFFF.toInt(), frame.borderColor)
        assertEquals(PolaroidPlacement(), frame.placement)
    }

    @Test
    fun windowSidePxForClassic() {
        assertEquals(892.857f, PolaroidGesture.windowSidePx(1000f, 2000f, PolaroidStyles.CLASSIC), 0.01f)
    }

    @Test
    fun dragPansPortraitPhotoByCropRatio() {
        // 4000x3000 at zoom 1: crop 3000, slack 500 per side. A 100 px drag on a 1000 px window
        // is 0.1 window, and 0.1 * 3000 / 500 = 0.6 in offset units.
        val result = PolaroidGesture.apply(PolaroidPlacement(), 100f, 0f, 1f, 1000f, 4000, 3000)
        assertEquals(-0.6f, result.offsetX, 0.001f)
        assertEquals(0f, result.offsetY, 0.001f)
    }

    @Test
    fun dragOnSquarePhotoAtZoomOneKeepsOffset() {
        val result = PolaroidGesture.apply(PolaroidPlacement(), 100f, 100f, 1f, 1000f, 1200, 1200)
        assertEquals(0f, result.offsetX, 0.001f)
        assertEquals(0f, result.offsetY, 0.001f)
    }

    @Test
    fun pinchClampsZoomToFour() {
        val result = PolaroidGesture.apply(PolaroidPlacement(), 0f, 0f, 10f, 1000f, 4000, 3000)
        assertEquals(4f, result.zoom, 0.001f)
    }

    @Test
    fun dragResultIsAlwaysInRange() {
        val result = PolaroidGesture.apply(PolaroidPlacement(), 1e6f, -1e6f, 1f, 1000f, 4000, 3000)
        assertTrue(abs(result.offsetX) <= 1f)
        assertTrue(abs(result.offsetY) <= 1f)
    }
}
