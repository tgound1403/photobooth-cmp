package com.example.photogether.core.moments

/**
 * Dominant colours of a photo, shown in the shot detail dialog.
 *
 * Pure Kotlin on purpose: it works on plain ARGB ints, so it runs on the JVM in unit tests.
 */
object ShotPalette {

    /** Bins per channel when colours are grouped: 4 bits each, so 16 x 16 x 16 buckets. */
    private const val BIN_SHIFT = 4
    private const val BINS = 1 shl (3 * (8 - BIN_SHIFT))

    /**
     * Returns up to [count] opaque ARGB colours, most common first.
     *
     * Pixels are grouped into coarse buckets and each bucket is averaged. A bucket is skipped when a colour
     * already chosen is closer than [minDistance] (RGB Euclidean), so the result is not five shades of one tone.
     */
    fun dominantColors(pixels: IntArray, count: Int = 5, minDistance: Int = 48): List<Int> {
        if (count <= 0 || pixels.isEmpty()) return emptyList()

        val hits = IntArray(BINS)
        val sumR = LongArray(BINS)
        val sumG = LongArray(BINS)
        val sumB = LongArray(BINS)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val bin = bin(r, g, b)
            hits[bin]++
            sumR[bin] += r.toLong()
            sumG[bin] += g.toLong()
            sumB[bin] += b.toLong()
        }

        val minDistanceSquared = minDistance.toLong() * minDistance
        val chosen = mutableListOf<IntArray>()
        val bins = (0 until BINS).filter { hits[it] > 0 }.sortedByDescending { hits[it] }
        for (bin in bins) {
            val n = hits[bin].toLong()
            val rgb = intArrayOf(
                (sumR[bin] / n).toInt(),
                (sumG[bin] / n).toInt(),
                (sumB[bin] / n).toInt(),
            )
            if (chosen.none { distanceSquared(it, rgb) < minDistanceSquared }) {
                chosen += rgb
                if (chosen.size == count) break
            }
        }
        return chosen.map { (0xFF shl 24) or (it[0] shl 16) or (it[1] shl 8) or it[2] }
    }

    private fun bin(r: Int, g: Int, b: Int): Int =
        ((r shr BIN_SHIFT) shl 8) or ((g shr BIN_SHIFT) shl 4) or (b shr BIN_SHIFT)

    private fun distanceSquared(a: IntArray, b: IntArray): Long {
        val dr = (a[0] - b[0]).toLong()
        val dg = (a[1] - b[1]).toLong()
        val db = (a[2] - b[2]).toLong()
        return dr * dr + dg * dg + db * db
    }
}
