package com.example.movietime.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Single source of truth for locale management.
 * On Android 13+ uses AppCompatDelegate.setApplicationLocales() (per-app language API).
 * On older versions uses wrap() in attachBaseContext.
 */
object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val PREF_LANG = "pref_lang"
    private const val DEFAULT_LANG = "uk"

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LANG, DEFAULT_LANG) ?: DEFAULT_LANG
    }

    fun getLocale(context: Context): Locale {
        return codeToLocale(getSavedLanguageCode(context))
    }

    fun codeToLocale(code: String): Locale = when (code) {
        "ru" -> Locale("ru")
        "en" -> Locale("en")
        else -> Locale("uk")
    }

    /**
     * Sets the application locale using the modern per-app language API.
     * Should be called when user changes language in settings.
     */
    fun setLocale(context: Context, langCode: String) {
        // Save to prefs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANG, langCode)
            .apply()

        // Apply using AppCompatDelegate (works on all API levels via AndroidX)
        val localeList = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Initializes locale on app startup.
     * Must be called in Application.onCreate() AFTER super.onCreate().
     */
    fun initLocale(context: Context) {
        val currentAppLocales = AppCompatDelegate.getApplicationLocales()
        val savedCode = getSavedLanguageCode(context)

        if (currentAppLocales.isEmpty) {
            // No locale set by system yet — apply saved preference
            val localeList = LocaleListCompat.forLanguageTags(savedCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        } else {
            // Sync saved pref with what the system has (e.g. user changed in system settings)
            val systemLang = currentAppLocales.toLanguageTags().split(",").firstOrNull()?.split("-")?.firstOrNull()
            if (systemLang != null && systemLang != savedCode && systemLang in listOf("uk", "ru", "en")) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_LANG, systemLang)
                    .apply()
            }
        }
    }

    /**
     * Wraps a context with the saved locale.
     * Use in every Activity attachBaseContext as a fallback for proper resource resolution.
     */
    fun wrap(context: Context): Context {
        val locale = getLocale(context)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    /**
     * Applies locale to Application resources.
     * Use in Application.onCreate and Application.onConfigurationChanged.
     */
    fun applyToApp(context: Context) {
        val locale = getLocale(context)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
