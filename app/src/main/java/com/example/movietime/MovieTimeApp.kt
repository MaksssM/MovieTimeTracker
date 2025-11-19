package com.example.movietime

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class MovieTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply saved theme from preferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val themePref = prefs.getString("pref_theme", "system") ?: "system"
        when (themePref) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Apply saved locale (default: English)
        val langPref = prefs.getString("pref_lang", "en") ?: "en"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val res = resources
        val config = Configuration(res.configuration)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        // Create configuration context to apply locale for the app
        createConfigurationContext(config)
    }
}
