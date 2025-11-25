package com.example.movietime.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.movietime.BuildConfig
import com.example.movietime.R
import com.google.android.material.materialswitch.MaterialSwitch
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs: SharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Load preferences
        val themePref = prefs.getString("pref_theme", "system") ?: "system"
        val langPref = prefs.getString("pref_lang", "uk") ?: "uk"
        val tmdbLangPref = prefs.getString("pref_tmdb_lang", "uk") ?: "uk"
        val showRatings = prefs.getBoolean("pref_show_ratings", true)
        val compactMode = prefs.getBoolean("pref_compact_mode", false)
        val autoplayTrailers = prefs.getBoolean("pref_autoplay_trailers", false)

        // Find views
        val radioThemeGroup = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioLangGroup = view.findViewById<RadioGroup>(R.id.radioGroupLanguage)
        val radioTmdbLangGroup = view.findViewById<RadioGroup>(R.id.radioGroupTmdbLanguage)
        val switchShowRatings = view.findViewById<MaterialSwitch>(R.id.switchShowRatings)
        val switchCompactMode = view.findViewById<MaterialSwitch>(R.id.switchCompactMode)
        val switchAutoplayTrailers = view.findViewById<MaterialSwitch>(R.id.switchAutoplayTrailers)
        val layoutClearCache = view.findViewById<LinearLayout>(R.id.layoutClearCache)
        val layoutExportData = view.findViewById<LinearLayout>(R.id.layoutExportData)
        val layoutClearAllData = view.findViewById<LinearLayout>(R.id.layoutClearAllData)
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)
        val tvCacheSize = view.findViewById<TextView>(R.id.tvCacheSize)

        // Show app version
        try {
            val versionName = BuildConfig.VERSION_NAME
            tvAppVersion.text = getString(R.string.app_version, versionName)
        } catch (e: Exception) {
            tvAppVersion.text = getString(R.string.app_version, "1.0.0")
        }

        // Calculate and show cache size
        updateCacheSize(tvCacheSize)

        // Set initial checked state for theme
        when (themePref) {
            "system" -> radioThemeGroup.check(R.id.radioThemeSystem)
            "light" -> radioThemeGroup.check(R.id.radioThemeLight)
            "dark" -> radioThemeGroup.check(R.id.radioThemeDark)
            else -> radioThemeGroup.check(R.id.radioThemeSystem)
        }

        // Set initial checked state for app language
        when (langPref) {
            "uk" -> radioLangGroup.check(R.id.radioLangUk)
            "ru" -> radioLangGroup.check(R.id.radioLangRu)
            "en" -> radioLangGroup.check(R.id.radioLangEn)
            else -> radioLangGroup.check(R.id.radioLangUk)
        }

        // Set initial checked state for TMDB content language
        when (tmdbLangPref) {
            "uk" -> radioTmdbLangGroup.check(R.id.radioTmdbLangUk)
            "ru" -> radioTmdbLangGroup.check(R.id.radioTmdbLangRu)
            "en" -> radioTmdbLangGroup.check(R.id.radioTmdbLangEn)
            else -> radioTmdbLangGroup.check(R.id.radioTmdbLangUk)
        }

        // Set initial switch states
        switchShowRatings.isChecked = showRatings
        switchCompactMode.isChecked = compactMode
        switchAutoplayTrailers.isChecked = autoplayTrailers

        // Theme change listener
        radioThemeGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioThemeSystem -> "system"
                R.id.radioThemeLight -> "light"
                R.id.radioThemeDark -> "dark"
                else -> "system"
            }
            prefs.edit { putString("pref_theme", value) }
            applyTheme(value)
            Toast.makeText(requireContext(), getString(R.string.theme_changed), Toast.LENGTH_SHORT).show()
        }

        // App language change listener
        radioLangGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioLangUk -> "uk"
                R.id.radioLangRu -> "ru"
                R.id.radioLangEn -> "en"
                else -> "uk"
            }
            prefs.edit { putString("pref_lang", value) }
            applyLocale(value)
            Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        // TMDB content language change listener
        radioTmdbLangGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioTmdbLangUk -> "uk"
                R.id.radioTmdbLangRu -> "ru"
                R.id.radioTmdbLangEn -> "en"
                else -> "uk"
            }
            prefs.edit { putString("pref_tmdb_lang", value) }
            Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
        }

        // Switch listeners
        switchShowRatings.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_show_ratings", isChecked) }
        }

        switchCompactMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_compact_mode", isChecked) }
        }

        switchAutoplayTrailers.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_autoplay_trailers", isChecked) }
        }

        // Clear cache
        layoutClearCache.setOnClickListener {
            viewModel.clearCache()
            updateCacheSize(tvCacheSize)
            Toast.makeText(requireContext(), getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
        }

        // Export data
        layoutExportData.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.export_data), Toast.LENGTH_SHORT).show()
            // TODO: Implement export functionality
        }

        // Clear all data
        layoutClearAllData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.clear_all_data))
                .setMessage(getString(R.string.clear_all_data_confirm))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    viewModel.clearAllData()
                    Toast.makeText(requireContext(), getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun updateCacheSize(tvCacheSize: TextView) {
        try {
            val cacheDir = requireContext().cacheDir
            val size = getFolderSize(cacheDir)
            val formattedSize = formatFileSize(size)
            tvCacheSize.text = getString(R.string.cache_size_format, formattedSize)
        } catch (e: Exception) {
            tvCacheSize.text = getString(R.string.cache_size_format, "0 KB")
        }
    }

    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = folder.length()
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    private fun applyTheme(value: String) {
        when (value) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun applyLocale(langCode: String) {
        val locale = when (langCode) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("uk")
        }
        Locale.setDefault(locale)
        val activity = requireActivity()
        val res = activity.resources
        val config = Configuration(res.configuration)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        activity.createConfigurationContext(config)
    }
}
