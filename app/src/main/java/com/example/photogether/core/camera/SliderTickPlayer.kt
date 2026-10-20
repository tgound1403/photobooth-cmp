package com.example.photogether.core.camera

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log
import kotlin.random.Random

/** The mechanical sounds the controls make. */
enum class Click(val asset: String, val volume: Float, val rate: Float = 1f) {
    /** Gear tooth of a horizontal ruler slider or dial. */
    SLIDER("sounds/slider_tick.wav", 0.6f),

    /** Ratchet detent of the round control wheel: the same sample played lower, so it sounds deeper than the sliders. */
    WHEEL("sounds/wheel_click.wav", 0.8f, rate = 0.7f),

    /** A mode switch snapping over. */
    SWITCH("sounds/switch_clunk.wav", 0.9f),
    ;

    /** Playback rate for this click with [jitter] in -1..1 applied, kept inside SoundPool's 0.5..2 range. */
    fun playbackRate(jitter: Float): Float = (rate * (1f + jitter.coerceIn(-1f, 1f) * RATE_JITTER)).coerceIn(0.5f, 2f)

    private companion object {
        /** Each click is played a little faster or slower so a run of detents does not sound like a loop. */
        const val RATE_JITTER = 0.06f
    }
}

/**
 * Plays the mechanical detent click of a slider or wheel each time its value moves one step, like a gear
 * catching a tooth. One process-wide [SoundPool] is shared by every control, so they do not each
 * decode the samples. Silent and vibrate modes stay quiet, the same as the shutter sound.
 */
object SliderTickPlayer {
    private const val TAG = "SliderTickPlayer"

    @Volatile
    private var pool: SoundPool? = null
    @Volatile
    private var soundIds: Map<Click, Int> = emptyMap()
    @Volatile
    private var audioManager: AudioManager? = null

    /** Loads the click the first time it is needed. Later calls return at once. Safe to call from any thread. */
    private fun ensureLoaded(context: Context): SoundPool? {
        pool?.let { return it }
        synchronized(this) {
            pool?.let { return it }
            return try {
                val appContext = context.applicationContext
                val created = SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .build()
                soundIds = Click.entries.associateWith { click ->
                    appContext.assets.openFd(click.asset).use { fd -> created.load(fd, 1) }
                }
                audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                pool = created
                created
            } catch (e: Exception) {
                Log.w(TAG, "Cannot load slider tick: ${e.message}", e)
                null
            }
        }
    }

    /** Plays one [click], unless the phone is in silent or vibrate mode. */
    fun play(context: Context, click: Click = Click.SLIDER) {
        val player = ensureLoaded(context) ?: return
        val id = soundIds[click] ?: return
        val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (!shouldPlay(enabled = true, ringerMode = ringer)) return
        val rate = click.playbackRate(Random.nextFloat() * 2f - 1f)
        player.play(id, click.volume, click.volume, 1, 0, rate)
    }

    private const val MAX_STREAMS = 4
}
