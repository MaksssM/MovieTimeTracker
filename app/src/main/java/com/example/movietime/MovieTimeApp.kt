package com.example.movietime

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.movietime.util.SecurityUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

import androidx.hilt.work.HiltWorkerFactory
// removed conflicting import

@HiltAndroidApp
class MovieTimeApp : Application(), androidx.work.Configuration.Provider, ImageLoaderFactory {
    
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
    
    // Global Coil ImageLoader configuration for better performance
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory for images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // Use 5% of available disk space
                    .build()
            }
            .respectCacheHeaders(false) // Ignore cache headers from TMDB for better caching
            .build()
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
        com.example.movietime.util.LocaleHelper.applyToApp(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        com.example.movietime.util.LocaleHelper.applyToApp(this)
    }
}
