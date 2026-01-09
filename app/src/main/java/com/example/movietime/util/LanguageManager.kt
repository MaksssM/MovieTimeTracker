package com.example.movietime.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized language manager for API calls.
 * Converts user's language preference to TMDB API language codes.
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /**
     * Get the current language code for TMDB API.
     * Returns full locale code like "uk-UA", "en-US", "ru-RU"
     */
    fun getApiLanguage(): String {
        return when (prefs.getString("pref_lang", "uk")) {
            "uk" -> "uk-UA"
            "en" -> "en-US"
            "ru" -> "ru-RU"
            else -> "uk-UA"
        }
    }

    /**
     * Get the region code for TMDB API.
     * Used for region-specific content like release dates.
     */
    fun getRegion(): String {
        return when (prefs.getString("pref_lang", "uk")) {
            "uk" -> "UA"
            "en" -> "US"
            "ru" -> "RU"
            else -> "UA"
        }
    }

    /**
     * Get simple language code (2-letter).
     */
    fun getLanguageCode(): String {
        return prefs.getString("pref_lang", "uk") ?: "uk"
    }

    companion object {
        const val PREF_KEY_LANGUAGE = "pref_lang"
        const val LANG_UKRAINIAN = "uk"
        const val LANG_ENGLISH = "en"
        const val LANG_RUSSIAN = "ru"
    }
}
