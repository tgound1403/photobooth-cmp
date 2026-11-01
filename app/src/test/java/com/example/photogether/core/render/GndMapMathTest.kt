package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Test

class GndMapMathTest {
    private val eps = 1e-4f

    private fun at(m: GndMap, u: Float, v: Float) = m.fromTop(u, v)

    @Test
    fun noTransformIsIdentity() {
        val m = GndMapMath.map(400, 300, CropParams())
        assertEquals(GndMap.IDENTITY, m)
    }

    @Test
    fun flipHorizontalDoesNotChangeVerticalGradient() {
        val m = GndMapMath.map(400, 300, CropParams(flipHorizontal = true))
        assertEquals(GndMap.IDENTITY, m)
    }

    @Test
    fun flipVerticalInvertsGradient() {
        val m = GndMapMath.map(400, 300, CropParams(flipVertical = true))
        assertEquals(1f, at(m, 0.3f, 0f), eps)
        assertEquals(0f, at(m, 0.3f, 1f), eps)
    }

    @Test
    fun oneQuarterTurnPutsSourceLeftAtTop() {
        // Clockwise 90: the source's left edge becomes the top.
        val m = GndMapMath.map(400, 300, CropParams(quarterTurns = 1))
        assertEquals(0f, at(m, 0f, 0.7f), eps)
        assertEquals(1f, at(m, 1f, 0.2f), eps)
    }

    @Test
    fun twoQuarterTurnsPutsSourceBottomAtTop() {
        val m = GndMapMath.map(400, 300, CropParams(quarterTurns = 2))
        assertEquals(0f, at(m, 0.5f, 1f), eps)
        assertEquals(1f, at(m, 0.5f, 0f), eps)
    }

    @Test
    fun threeQuarterTurnsPutsSourceRightAtTop() {
        val m = GndMapMath.map(400, 300, CropParams(quarterTurns = 3))
        assertEquals(0f, at(m, 1f, 0.4f), eps)
        assertEquals(1f, at(m, 0f, 0.4f), eps)
    }

    @Test
    fun rotationWithFlipsAgreesWithMatrixComposition() {
        // flipH then 1 turn: source (u, v) -> flipped u' = 1 - u -> fromTop = u' = 1 - u.
        val m = GndMapMath.map(400, 300, CropParams(quarterTurns = 1, flipHorizontal = true))
        assertEquals(1f, at(m, 0f, 0.5f), eps)
        assertEquals(0f, at(m, 1f, 0.5f), eps)
        // flipV then 1 turn: the vertical flip only changes v, which no longer drives fromTop.
        val n = GndMapMath.map(400, 300, CropParams(quarterTurns = 1, flipVertical = true))
        assertEquals(0f, at(n, 0f, 0.5f), eps)
    }

    @Test
    fun aspectCropMeasuresFadeInVisibleFrame() {
        // 400x300 cropped square: keeps full height? 4:3 is wider than 1:1, so the sides are trimmed.
        val wide = GndMapMath.map(400, 300, CropParams(aspect = CropAspect.SQUARE))
        assertEquals(GndMap.IDENTITY, wide)
        // 300x400 cropped square: full width, rows 50..350 stay -> visible top at v = 0.125.
        val tall = GndMapMath.map(300, 400, CropParams(aspect = CropAspect.SQUARE))
        assertEquals(0f, at(tall, 0.5f, 0.125f), eps)
        assertEquals(1f, at(tall, 0.5f, 0.875f), eps)
        assertEquals(0.5f, at(tall, 0.5f, 0.5f), eps)
    }

    @Test
    fun aspectCropAfterRotation() {
        // 400x300 turned once is 300x400; square crop keeps rotated rows 50..350. fromTop = u mapped.
        val m = GndMapMath.map(400, 300, CropParams(aspect = CropAspect.SQUARE, quarterTurns = 1))
        assertEquals(0f, at(m, 0.125f, 0.5f), eps)
        assertEquals(1f, at(m, 0.875f, 0.5f), eps)
    }
}
