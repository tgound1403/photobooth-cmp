package com.example.photogether.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomLayersTest {
    @Test
    fun noEffectGivesNoLayers() {
        assertTrue(BloomMath.layersFor(EditParams()).isEmpty())
    }

    @Test
    fun halationKeepsTheLivePassNumbers() {
        val layer = BloomMath.layersFor(EditParams(halation = 0.5f)).single()
        assertEquals(BloomMath.KNEE_START, layer.kneeStart, 0f)
        assertEquals(BloomMath.KNEE_END, layer.kneeEnd, 0f)
        assertEquals(1f, layer.spread, 0f)
        assertEquals(BloomMath.TINT_R * 0.5f, layer.gainR, 1e-6f)
        assertEquals(BloomMath.TINT_B * 0.5f, layer.gainB, 1e-6f)
    }

    @Test
    fun eachEffectAddsOneLayerAndScalesWithItsSlider() {
        val all = BloomMath.layersFor(EditParams(halation = 1f, bloom = 1f, glow = 1f, dreamy = 1f))
        assertEquals(4, all.size)
        val half = BloomMath.layersFor(EditParams(bloom = 0.5f)).single()
        val full = BloomMath.layersFor(EditParams(bloom = 1f)).single()
        assertEquals(full.gainR / 2f, half.gainR, 1e-6f)
    }

    @Test
    fun newEffectsCountAsEdits() {
        assertTrue(!EditParams(dreamy = 0.2f).isIdentity)
        assertEquals(1f, EditParams(glow = 4f).clamped().glow, 0f)
    }
}
