package com.example.photogether.core.camera

import android.content.Context
import android.util.Log

/** Which exposure control is the big round wheel on the landscape plate. The other two become horizontal dials. */
enum class RoundWheel(val key: String) {
    EV("ev"),
    ISO("iso"),
    TV("tv"),
    ;

    /** The two controls shown as horizontal dials when this one is the wheel, top to bottom: shutter, ISO, EV. */
    fun dials(): List<RoundWheel> = DIAL_ORDER.filter { it != this }

    companion object {
        val DEFAULT = EV
        private val DIAL_ORDER = listOf(TV, ISO, EV)

        /** The choice stored as [key], or [DEFAULT] for null or an unknown value. */
        fun fromKey(key: String?): RoundWheel = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** The round wheel choice, kept in the app's SharedPreferences. Defaults to [RoundWheel.EV]. */
object RoundWheelPrefs {
    const val KEY = "round_wheel"
    private const val TAG = "RoundWheelPrefs"

    fun read(context: Context): RoundWheel =
        try {
            RoundWheel.fromKey(context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null))
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read round wheel setting, using EV: ${e.message}", e)
            RoundWheel.DEFAULT
        }

    fun write(context: Context, wheel: RoundWheel) {
        try {
            context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY, wheel.key).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot save round wheel setting: ${e.message}", e)
        }
    }
}

/** The "rotate to Pro mode" tip on the portrait camera: shown once, until it is dismissed. */
object RotateTipPrefs {
    const val KEY_SEEN = "rotate_tip_seen"
    private const val TAG = "RotateTipPrefs"

    /** True when the tip should show: it has not been dismissed, or the flag cannot be read. */
    fun shouldShow(context: Context): Boolean =
        try {
            !context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read tip flag, showing the tip: ${e.message}", e)
            true
        }

    fun markSeen(context: Context) {
        try {
            context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEEN, true).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot save tip flag: ${e.message}", e)
        }
    }
}
