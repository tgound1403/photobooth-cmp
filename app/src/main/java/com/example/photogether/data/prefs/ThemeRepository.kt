package com.example.photogether.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The dark theme setting for the paper screens. Kept in SharedPreferences so the value is read
 * synchronously at start and the first frame already uses the saved theme.
 */
class ThemeRepository(private val prefs: SharedPreferences) {

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, false))

    /** True when the paper screens use the dark palette. */
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    /** Saves [enabled] and updates [darkTheme] right away. */
    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "photogether_prefs"
        private const val KEY_DARK_THEME = "dark_theme_enabled"

        fun create(context: Context): ThemeRepository =
            ThemeRepository(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
