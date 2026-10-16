package com.example.photogether.core.perf

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import com.tgound.photogether.BuildConfig

/**
 * Debug-only frame jank baseline. Wraps JankStats for one window.
 * Janky frames and a 5-second summary are logged under tag "Jank" in debug builds only.
 * Call [resume] from onResume and [pause] from onPause.
 */
class JankLogger(window: Window) {

    private val handler = Handler(Looper.getMainLooper())
    private var total = 0
    private var janky = 0
    private var running = false

    private val jankStats: JankStats = JankStats.createAndTrack(window) { frameData ->
        onFrame(frameData)
    }.also { it.isTrackingEnabled = false }

    private val summaryRunnable = object : Runnable {
        override fun run() {
            logSummary()
            if (running) handler.postDelayed(this, SUMMARY_INTERVAL_MS)
        }
    }

    fun resume() {
        if (!BuildConfig.DEBUG || running) return
        running = true
        jankStats.isTrackingEnabled = true
        handler.postDelayed(summaryRunnable, SUMMARY_INTERVAL_MS)
    }

    fun pause() {
        if (!BuildConfig.DEBUG || !running) return
        running = false
        jankStats.isTrackingEnabled = false
        handler.removeCallbacks(summaryRunnable)
        logSummary()
    }

    private fun onFrame(frameData: FrameData) {
        total++
        if (frameData.isJank) {
            janky++
            Log.w(
                TAG,
                "Janky frame: ${frameData.frameDurationUiNanos / 1_000_000f} ms " +
                    "states=${frameData.states.joinToString { "${it.key}=${it.value}" }}"
            )
        }
    }

    /** Logs the counts since the last summary, then resets them. */
    private fun logSummary() {
        if (total == 0) return
        val pct = janky * 100.0 / total
        Log.i(TAG, "Jank summary: total=$total janky=$janky pct=${"%.1f".format(pct)}%")
        total = 0
        janky = 0
    }

    private companion object {
        const val TAG = "Jank"
        const val SUMMARY_INTERVAL_MS = 5_000L
    }
}
