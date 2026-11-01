package com.example.photogether.core.render

/**
 * Maps a source-image UV to the "distance from the top" of the final photo, so the graduated ND
 * always darkens the top of the visible frame. The GPU grade runs on the whole original bitmap and
 * [CropRenderer] flips, rotates and crops afterwards, so the shader needs this to know where the
 * visible top is. fromTop = [du] * u + [dv] * v + [offset], with (u, v) the source UV (v = 0 at the
 * source top). fromTop is 0 at the visible top edge and 1 at the visible bottom edge; outside the
 * visible crop it goes below 0 or above 1.
 */
data class GndMap(val du: Float, val dv: Float, val offset: Float) {
    fun fromTop(u: Float, v: Float): Float = du * u + dv * v + offset

    companion object {
        /** No transform: fromTop = v. */
        val IDENTITY = GndMap(0f, 1f, 0f)
    }
}

object GndMapMath {

    private data class Affine(val a: Float, val b: Float, val c: Float) // a*u + b*v + c

    /**
     * Mirrors [CropRenderer.apply] for a [srcW] x [srcH] source: flipHorizontal, then flipVertical, then a
     * clockwise rotation of [CropParams.quarterTurns], then the centred aspect crop from [CropMath.cropRect].
     */
    fun map(srcW: Int, srcH: Int, crop: CropParams): GndMap {
        val c = crop.normalized()
        // Normalised coordinates after the flips (y down, 0..1).
        val u1 = if (c.flipHorizontal) Affine(-1f, 0f, 1f) else Affine(1f, 0f, 0f)
        val v1 = if (c.flipVertical) Affine(0f, -1f, 1f) else Affine(0f, 1f, 0f)
        // Clockwise rotation: 1 -> (u, v) = (1 - v1, u1); 2 -> (1 - u1, 1 - v1); 3 -> (v1, 1 - u1).
        val v2 =
            when (c.quarterTurns) {
                1 -> u1
                2 -> v1.inverted()
                3 -> u1.inverted()
                else -> v1
            }
        val (rotW, rotH) = CropMath.rotatedSize(srcW, srcH, c.quarterTurns)
        val rect = CropMath.cropRect(rotW, rotH, c.aspect)
        val scale = rotH.toFloat() / rect.height
        val shift = rect.top.toFloat() / rect.height
        return GndMap(v2.a * scale, v2.b * scale, v2.c * scale - shift)
    }

    private fun Affine.inverted() = Affine(-a, -b, 1f - c)
}
