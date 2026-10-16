package com.example.photogether.core.camera

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log
import com.example.photogether.core.render.ShutterSound

/**
 * True when the shutter sound should play: the user has it on and the phone is in normal ringer mode.
 * Silent and vibrate modes stay quiet.
 */
fun shouldPlay(enabled: Boolean, ringerMode: Int): Boolean =
    enabled && ringerMode == AudioManager.RINGER_MODE_NORMAL

/**
 * Plays the shutter sound of the camera body on capture. All three sounds are decoded into a [SoundPool]
 * when the player is created, so a shot plays without waiting for I/O.
 */
class ShutterSoundPlayer(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = HashMap<ShutterSound, Int>()
    private val pending = HashMap<Int, AssetFileDescriptor>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            // Each descriptor is closed once SoundPool has read it.
            pending.remove(sampleId)?.close()
            if (status != 0) Log.w(TAG, "Shutter sound $sampleId failed to load, status $status")
        }
        ShutterSound.entries.forEach { sound ->
            try {
                val fd = context.assets.openFd(sound.asset)
                val id = soundPool.load(fd, 1)
                soundIds[sound] = id
                pending[id] = fd
            } catch (e: Exception) {
                Log.w(TAG, "Cannot load shutter sound ${sound.asset}: ${e.message}", e)
            }
        }
    }

    /** Plays [sound] when [shouldPlay] allows it for this ringer mode. Does nothing otherwise. */
    fun play(sound: ShutterSound, enabled: Boolean, ringerMode: Int) {
        if (!shouldPlay(enabled, ringerMode)) return
        val id = soundIds[sound] ?: return
        val stream = soundPool.play(id, VOLUME, VOLUME, 1, 0, 1f)
        if (stream == 0) Log.w(TAG, "SoundPool did not play ${sound.asset} (not loaded yet?)")
    }

    /** Frees the sound pool. The player must not be used after this. */
    fun release() {
        pending.values.forEach { runCatching { it.close() } }
        pending.clear()
        soundIds.clear()
        soundPool.release()
    }

    private companion object {
        const val TAG = "ShutterSoundPlayer"
        const val MAX_STREAMS = 2
        const val VOLUME = 1f
    }
}

/** The on/off switch for the shutter sound, kept in the app's SharedPreferences. Defaults to on. */
object ShutterSoundPrefs {
    const val PREFS_NAME = "photogether_prefs"
    const val KEY_ENABLED = "shutter_sound_enabled"
    private const val TAG = "ShutterSoundPrefs"

    /** Saved value, or true when it cannot be read. */
    fun read(context: Context): Boolean =
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read shutter sound setting, using on: ${e.message}", e)
            true
        }

    /** Saves [enabled]. A failure is logged and otherwise ignored; the in-memory value still changes. */
    fun write(context: Context, enabled: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot save shutter sound setting: ${e.message}", e)
        }
    }
}
