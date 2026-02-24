package com.example.movietime.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Single source of truth for locale management.
 * Use wrap() in attachBaseContext, apply() in Application.onCreate/onConfigurationChanged.
 */
object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val PREF_LANG = "pref_lang"
    private const val DEFAULT_LANG = "uk"

    fun getLocale(context: Context): Locale {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return codeToLocale(prefs.getString(PREF_LANG, DEFAULT_LANG) ?: DEFAULT_LANG)
    }

    fun codeToLocale(code: String): Locale = when (code) {
        "ru" -> Locale("ru")
        "en" -> Locale("en")
        else -> Locale("uk")
    }

    /**
     * Wraps a context with the saved locale.
     * Use in every Activity/Fragment host attachBaseContext.
     */
    fun wrap(context: Context): Context {
        val locale = getLocale(context)
        Locale.setDefault(locale)
        android.os.LocaleList.setDefault(android.os.LocaleList(locale))
        val config = Configuration(context.resources.configuration)
        config.setLocales(android.os.LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    /**
     * Applies locale to Application resources (for Application-level strings).
     * Use in Application.onCreate and Application.onConfigurationChanged.
     */
    fun applyToApp(context: Context) {
        val locale = getLocale(context)
        Locale.setDefault(locale)
        android.os.LocaleList.setDefault(android.os.LocaleList(locale))
        val config = Configuration(context.resources.configuration)
        config.setLocales(android.os.LocaleList(locale))
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
