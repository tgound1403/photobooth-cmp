package com.example.photogether.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.photogether.core.moments.CalendarColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The look settings: dark theme for the paper screens, and the colour of the home calendar. Kept in SharedPreferences so the value is read
 * synchronously at start and the first frame already uses the saved theme.
 */
class ThemeRepository(private val prefs: SharedPreferences) {

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, false))

    /** True when the paper screens use the dark palette. */
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _calendarColor = MutableStateFlow(
        CalendarColors.opaque(prefs.getInt(KEY_CALENDAR_COLOR, CalendarColors.DEFAULT)),
    )

    /** ARGB colour of the home calendar sheet. */
    val calendarColor: StateFlow<Int> = _calendarColor.asStateFlow()

    /** Saves the calendar colour (made opaque) and updates [calendarColor] right away. */
    fun setCalendarColor(argb: Int) {
        val color = CalendarColors.opaque(argb)
        _calendarColor.value = color
        prefs.edit().putInt(KEY_CALENDAR_COLOR, color).apply()
    }

    /** Saves [enabled] and updates [darkTheme] right away. */
    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "photogether_prefs"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_CALENDAR_COLOR = "calendar_color"

        fun create(context: Context): ThemeRepository =
            ThemeRepository(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
