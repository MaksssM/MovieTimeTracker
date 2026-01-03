package com.example.movietime

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.movietime.util.SecurityUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

import androidx.hilt.work.HiltWorkerFactory
// removed conflicting import

@HiltAndroidApp
class MovieTimeApp : Application(), androidx.work.Configuration.Provider {
    
    @javax.inject.Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    companion object {
        private const val TAG = "MovieTimeApp"
    }
    
    override fun onCreate() {
        super.onCreate()

        // Security checks (only log in debug, don't block users)
        performSecurityChecks()

        // Apply saved theme from preferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val themePref = prefs.getString("pref_theme", "system") ?: "system"
        when (themePref) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Apply saved locale (default: Ukrainian)
        applyLocale()
    }

    /**
     * Perform security checks at app startup.
     * In production, you might want to restrict functionality or log analytics.
     */
    private fun performSecurityChecks() {
        if (BuildConfig.DEBUG) {
            // Only log in debug builds
            if (SecurityUtils.isDeviceRooted()) {
                Log.w(TAG, "Security: Device appears to be rooted")
            }
            if (SecurityUtils.isEmulator()) {
                Log.w(TAG, "Security: Running on emulator")
            }
            if (SecurityUtils.isDebuggerAttached()) {
                Log.w(TAG, "Security: Debugger is attached")
            }
        } else {
            // In release builds, store security status for analytics (without blocking)
            val securePrefs = SecurityUtils.getSecurePreferences(this)
            securePrefs.edit().apply {
                putBoolean("security_rooted", SecurityUtils.isDeviceRooted())
                putBoolean("security_emulator", SecurityUtils.isEmulator())
                putLong("security_check_time", System.currentTimeMillis())
                apply()
            }
        }
    }

    private fun applyLocale() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "uk") ?: "uk"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("uk")
        }
        Locale.setDefault(locale)
        val res = resources
        val config = Configuration(res.configuration)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}
