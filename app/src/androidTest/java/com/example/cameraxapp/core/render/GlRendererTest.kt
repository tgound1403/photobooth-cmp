package com.example.cameraxapp.core.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlRendererTest {

    private lateinit var renderer: GlRenderer

    @Before
    fun setUp() {
        renderer = GlRenderer()
    }

    @After
    fun tearDown() {
        renderer.close()
    }

    /** 64x64 gradient. maxSize = 64 forces the GPU path even for identity params. */
    private fun gradient(): Bitmap {
        val pixels = IntArray(64 * 64)
        for (y in 0 until 64) for (x in 0 until 64) {
            pixels[y * 64 + x] = Color.rgb(x * 4, y * 4, (x + y) * 2)
        }
        return Bitmap.createBitmap(pixels, 64, 64, Bitmap.Config.ARGB_8888)
    }

    /** Identity LUT written with the layout from generate_luts.py. */
    private fun identityLut(): Bitmap {
        fun level(v: Int) = (v * 255f / 63f).roundToInt()
        val pixels = IntArray(512 * 512)
        for (b in 0 until 64) for (g in 0 until 64) for (r in 0 until 64) {
            val x = (b % 8) * 64 + r
            val y = (b / 8) * 64 + g
            pixels[y * 512 + x] = Color.rgb(level(r), level(g), level(b))
        }
        return Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888)
    }

    private fun assertClose(expected: Bitmap, actual: Bitmap, tolerance: Int) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        val a = IntArray(expected.width * expected.height)
        val b = IntArray(actual.width * actual.height)
        expected.getPixels(a, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(b, 0, actual.width, 0, 0, actual.width, actual.height)
        for (i in a.indices) {
            val dr = abs(Color.red(a[i]) - Color.red(b[i]))
            val dg = abs(Color.green(a[i]) - Color.green(b[i]))
            val db = abs(Color.blue(a[i]) - Color.blue(b[i]))
            assertTrue("pixel $i differs by ($dr, $dg, $db)", dr <= tolerance && dg <= tolerance && db <= tolerance)
        }
    }

    private fun meanLuma(bitmap: Bitmap): Double {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return px.sumOf { 0.2126 * Color.red(it) + 0.7152 * Color.green(it) + 0.0722 * Color.blue(it) } / px.size
    }

    @Test
    fun defaultParams_matchSourceWithinTwo() = runBlocking {
        val src = gradient()
        val out = renderer.render(src, EditParams(), null, maxSize = 64)
        assertClose(src, out, 2)
    }

    @Test
    fun identityLut_fullIntensity_matchesSourceWithinTwo() = runBlocking {
        val src = gradient()
        val params = EditParams(filterId = "test-identity", filterIntensity = 1f)
        val out = renderer.render(src, params, identityLut(), maxSize = 64)
        assertClose(src, out, 2)
    }

    @Test
    fun exposurePlusOne_brightensMeanLuma() = runBlocking {
        val src = gradient()
        val base = renderer.render(src, EditParams(), null, maxSize = 64)
        val bright = renderer.render(src, EditParams(exposure = 1f), null, maxSize = 64)
        assertTrue(meanLuma(bright) > meanLuma(base))
    }

    @Test
    fun saturationMinusOne_givesGray() = runBlocking {
        val src = gradient()
        val out = renderer.render(src, EditParams(saturation = -1f), null, maxSize = 64)
        val px = IntArray(out.width * out.height)
        out.getPixels(px, 0, out.width, 0, 0, out.width, out.height)
        for (p in px) {
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            assertTrue("R=$r G=$g B=$b", abs(r - g) <= 3 && abs(g - b) <= 3)
        }
    }
}
