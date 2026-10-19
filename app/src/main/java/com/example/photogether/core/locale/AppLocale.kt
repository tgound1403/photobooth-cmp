package com.example.photogether.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** Languages the app ships strings for. Keep in sync with res/xml/locales_config.xml. */
enum class AppLanguage(val tag: String, val code: String, val nativeName: String) {
    VIETNAMESE("vi", "VI", "Tiếng Việt"),
    ENGLISH("en", "EN", "English");

    companion object {
        fun fromTag(tag: String?): AppLanguage? =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }

        /**
         * The language to preselect: an explicit per-app choice wins, then the device language,
         * then English.
         */
        fun resolve(appLanguageTag: String?, systemLanguageTag: String?): AppLanguage =
            fromTag(appLanguageTag) ?: fromTag(systemLanguageTag) ?: ENGLISH
    }
}

object AppLocale {
    /** Language currently applied to the app. */
    fun current(): AppLanguage {
        val explicit = AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.language
        return AppLanguage.resolve(explicit, Locale.getDefault().language)
    }

    /** Applies [language] app-wide. Recreates the running activity when it changes. */
    fun apply(language: AppLanguage) {
        if (current() == language && !AppCompatDelegate.getApplicationLocales().isEmpty) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
