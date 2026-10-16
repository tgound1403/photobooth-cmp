package com.example.photogether.core.render

/** Pure maths for the light leak and dust overlays: deterministic per-seed placement and strength scaling. */
object FxMath {
    private const val LEAK_SALT = 0x1EAC1EAC
    private const val DUST_SALT = 0x0D057D05
    private const val FLIP_SALT = 0x0F11F0F1
    private const val SEED_MULTIPLIER = 0x9E3779B1.toInt()

    /** Offset of the leak texture, each component in [0, 1). Same seed, same result. */
    fun leakOffset(seed: Int): Pair<Float, Float> = offset(seed xor LEAK_SALT)

    /** Offset of the dust texture, each component in [0, 1). Uses a different salt than [leakOffset]. */
    fun dustOffset(seed: Int): Pair<Float, Float> = offset(seed xor DUST_SALT)

    /** Whether the leak is mirrored horizontally for this seed. */
    fun leakFlip(seed: Int): Boolean = (hash(seed xor FLIP_SALT) and 1) == 1

    /** Leak strength uniform: clamped to [0, 1], 0 for non-positive input. */
    fun leakScale(strength: Float): Float = if (strength <= 0f) 0f else strength.coerceAtMost(1f)

    /** Dust strength uniform: clamped to [0, 1], 0 for non-positive input. */
    fun dustScale(strength: Float): Float = if (strength <= 0f) 0f else strength.coerceAtMost(1f)

    /** A non-negative seed for one capture. Consecutive timestamps give different seeds. */
    fun newCaptureSeed(nowNs: Long): Int {
        val folded = (nowNs xor (nowNs ushr 32)).toInt()
        return (folded * SEED_MULTIPLIER) and 0x7fffffff
    }

    private fun offset(key: Int): Pair<Float, Float> {
        val h1 = hash(key)
        val h2 = hash(h1 + 0x632BE5AB)
        return unit(h1) to unit(h2)
    }

    /** 24 random bits as a float in [0, 1). */
    private fun unit(h: Int): Float = (h ushr 8) / 16777216f

    /** Integer finalizer (lowbias32). */
    private fun hash(x: Int): Int {
        var h = x
        h = h xor (h ushr 16)
        h *= 0x7feb352d
        h = h xor (h ushr 15)
        h *= 0x846ca68b.toInt()
        h = h xor (h ushr 16)
        return h
    }
}
