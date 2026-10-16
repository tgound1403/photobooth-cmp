package com.example.photogether.core.render

/**
 * Timing for the live LUT crossfade. Pure math, driven by the caller's clock (System.nanoTime).
 * [mixAt] is 1 until [start] is called, then a smoothstep from 0 to 1 over [durationNs].
 */
internal class LutCrossfade(private val durationNs: Long) {
    private var startNs = 0L
    private var running = false

    fun start(nowNs: Long) {
        startNs = nowNs
        running = true
    }

    fun mixAt(nowNs: Long): Float {
        if (!running) return 1f
        val t = ((nowNs - startNs).toDouble() / durationNs).coerceIn(0.0, 1.0)
        if (t >= 1.0) running = false
        return (t * t * (3.0 - 2.0 * t)).toFloat()
    }
}
