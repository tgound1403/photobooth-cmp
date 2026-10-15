package com.example.cameraxapp.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PolaroidGeometryTest {

    private val sizes = listOf(
        "portrait" to (1000 to 1500),
        "landscape" to (1600 to 900),
        "square" to (1200 to 1200),
    )

    private fun inside(inner: FloatRect, outer: FloatRect): Boolean {
        val eps = 0.01f
        return inner.left >= outer.left - eps && inner.top >= outer.top - eps &&
            inner.right <= outer.right + eps && inner.bottom <= outer.bottom + eps
    }

    @Test
    fun fitShowsWholePhotoWithoutCropping() {
        for ((name, size) in sizes) {
            val (w, h) = size
            val layout = PolaroidGeometry.compute(w, h, PolaroidMode.FIT)
            // The source rectangle must be the entire photo.
            assertEquals("$name source", FloatRect(0f, 0f, w.toFloat(), h.toFloat()), layout.sourceRect)
            // The photo must sit inside the window and keep its aspect ratio.
            assertTrue("$name inside window", inside(layout.photoDest, layout.photoArea))
            val destRatio = layout.photoDest.width / layout.photoDest.height
            assertEquals("$name aspect", w.toFloat() / h, destRatio, 0.001f)
            // The longer side of the photo touches the window edge, so nothing is wasted or cut.
            val side = layout.photoArea.width
            assertTrue("$name fills one dimension", abs(layout.photoDest.width - side) < 0.01f || abs(layout.photoDest.height - side) < 0.01f)
        }
    }

    @Test
    fun fitFrameIsLargeEnoughForEveryOrientation() {
        for ((name, size) in sizes) {
            val (w, h) = size
            val layout = PolaroidGeometry.compute(w, h, PolaroidMode.FIT)
            assertTrue("$name frame width", layout.frameWidth >= layout.photoArea.width)
            assertTrue("$name frame height", layout.frameHeight > layout.photoArea.bottom)
        }
    }

    @Test
    fun fillCropsCentredSquareOfSource() {
        for ((name, size) in sizes) {
            val (w, h) = size
            val layout = PolaroidGeometry.compute(w, h, PolaroidMode.FILL)
            val src = layout.sourceRect
            assertEquals("$name square source", src.width, src.height, 0.001f)
            assertTrue("$name source inside photo", src.left >= 0 && src.top >= 0 && src.right <= w && src.bottom <= h)
            assertEquals("$name fills window", layout.photoArea, layout.photoDest)
        }
    }

    @Test
    fun fillCentresTheCropOnLandscape() {
        val layout = PolaroidGeometry.compute(1600, 900, PolaroidMode.FILL)
        assertEquals(350f, layout.sourceRect.left, 0.001f)
        assertEquals(0f, layout.sourceRect.top, 0.001f)
    }
}
