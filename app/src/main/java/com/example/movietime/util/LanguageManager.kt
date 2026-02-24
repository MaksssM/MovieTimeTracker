package com.example.movietime.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единый центр управления языком приложения.
 * Одна настройка pref_lang управляет и UI-локалью и языком TMDB API.
 *
 * При смене языка устанавливается флаг needs_library_refresh = true.
 * При следующем запуске AppRepository проверяет флаг и обновляет
 * title/overview библиотечных элементов из API.
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // ── API language ────────────────────────────────────────────────

    /** Полный TMDB-код языка: "uk-UA", "en-US", "ru-RU". */
    fun getApiLanguage(): String {
        return when (getLanguageCode()) {
            "uk" -> "uk-UA"
            "en" -> "en-US"
            "ru" -> "ru-RU"
            else -> "uk-UA"
        }
    }

    /** Регион для TMDB API (релизные даты и т.д.). */
    fun getRegion(): String {
        return when (getLanguageCode()) {
            "uk" -> "UA"
            "en" -> "US"
            "ru" -> "RU"
            else -> "UA"
        }
    }

    /** Двухбуквенный код текущего языка. */
    fun getLanguageCode(): String {
        return prefs.getString(PREF_KEY_LANGUAGE, LANG_UKRAINIAN) ?: LANG_UKRAINIAN
    }

    // ── Флаг отложенного обновления библиотеки ──────────────────────

    /** true → при следующем старте нужно пере-запросить title/overview из API. */
    fun isLibraryRefreshNeeded(): Boolean =
        prefs.getBoolean(PREF_KEY_NEEDS_REFRESH, false)

    /** Установить флаг «нужен refresh». Вызывается при смене языка. */
    fun setLibraryRefreshNeeded() {
        prefs.edit { putBoolean(PREF_KEY_NEEDS_REFRESH, true) }
    }

    /** Снять флаг после успешного обновления. */
    fun clearLibraryRefreshFlag() {
        prefs.edit { putBoolean(PREF_KEY_NEEDS_REFRESH, false) }
    }

    companion object {
        const val PREF_KEY_LANGUAGE = "pref_lang"
        const val PREF_KEY_NEEDS_REFRESH = "needs_library_refresh"
        const val LANG_UKRAINIAN = "uk"
        const val LANG_ENGLISH = "en"
        const val LANG_RUSSIAN = "ru"
    }
}
