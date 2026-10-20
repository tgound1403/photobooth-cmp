package com.example.photogether.core.camera

import android.content.Context
import android.util.Log

/** The rule-of-thirds grid switch, kept in the app's SharedPreferences. Defaults to off. */
object GridPrefs {
    const val KEY_ENABLED = "grid_enabled"
    private const val TAG = "GridPrefs"

    /** Saved value, or false when it cannot be read. */
    fun read(context: Context): Boolean =
        try {
            context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read grid setting, using off: ${e.message}", e)
            false
        }

    /** Saves [enabled]. A failure is logged and otherwise ignored. */
    fun write(context: Context, enabled: Boolean) {
        try {
            context.getSharedPreferences(ShutterSoundPrefs.PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot save grid setting: ${e.message}", e)
        }
    }
}
