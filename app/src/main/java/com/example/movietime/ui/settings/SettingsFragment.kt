package com.example.movietime.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.movietime.BuildConfig
import com.example.movietime.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var prefs: SharedPreferences

    // Views
    private lateinit var tvCurrentLanguage: TextView
    private lateinit var tvCurrentContentLanguage: TextView
    private lateinit var tvCurrentTheme: TextView
    private lateinit var tvCacheSize: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Initialize Views
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage)
        tvCurrentContentLanguage = view.findViewById(R.id.tvCurrentContentLanguage)
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme)
        tvCacheSize = view.findViewById(R.id.tvCacheSize)
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)

        val optLanguage = view.findViewById<LinearLayout>(R.id.optLanguage)
        val optContentLanguage = view.findViewById<LinearLayout>(R.id.optContentLanguage)
        val optTheme = view.findViewById<LinearLayout>(R.id.optTheme)
        val optClearCache = view.findViewById<LinearLayout>(R.id.optClearCache)

        val switchCompactMode = view.findViewById<SwitchMaterial>(R.id.switchCompactMode)
        val switchShowRatings = view.findViewById<SwitchMaterial>(R.id.switchShowRatings)
        val switchNotifyEpisodes = view.findViewById<SwitchMaterial>(R.id.switchNotifyEpisodes)
        val switchNotifyDigest = view.findViewById<SwitchMaterial>(R.id.switchNotifyDigest)

        // Load and Display Current Values
        updateLanguageText()
        updateContentLanguageText()
        updateThemeText()
        updateCacheSize()
        
        // Show App Version
        try {
            tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        } catch (e: Exception) {
            tvAppVersion.text = getString(R.string.app_version, "1.0.0")
        }

        // Initialize Switches
        switchCompactMode.isChecked = prefs.getBoolean("pref_compact_mode", false)
        switchShowRatings.isChecked = prefs.getBoolean("pref_show_ratings", true)
        switchNotifyEpisodes.isChecked = prefs.getBoolean("pref_notify_episodes", true)
        switchNotifyDigest.isChecked = prefs.getBoolean("pref_notify_digest", false)

        // Set Listeners
        
        // Dialogs
        optLanguage.setOnClickListener { showLanguageDialog() }
        optContentLanguage.setOnClickListener { showContentLanguageDialog() }
        optTheme.setOnClickListener { showThemeDialog() }
        optClearCache.setOnClickListener { clearCache() }

        // Switches

        switchCompactMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_compact_mode", isChecked) }
            // Notify user that restart might be needed or just let it update on next bind
        }

        switchShowRatings.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_show_ratings", isChecked) }
        }


        switchNotifyEpisodes.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_notify_episodes", isChecked) }
        }

        switchNotifyDigest.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_notify_digest", isChecked) }
        }
    }

    private fun updateLanguageText() {
        val langCode = prefs.getString("pref_lang", "uk") ?: "uk"
        tvCurrentLanguage.text = when (langCode) {
            "uk" -> getString(R.string.lang_uk)
            "ru" -> getString(R.string.lang_ru)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_uk)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_uk), getString(R.string.lang_ru), getString(R.string.lang_en))
        val codes = arrayOf("uk", "ru", "en")
        
        val currentCode = prefs.getString("pref_lang", "uk")
        val checkedItem = codes.indexOf(currentCode).takeIf { it != -1 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.language_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                if (selectedCode != currentCode) {
                    prefs.edit { putString("pref_lang", selectedCode) }
                    updateLanguageText()
                    applyLocale(selectedCode)
                    requireActivity().recreate()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateContentLanguageText() {
        val langCode = prefs.getString("pref_tmdb_lang", "uk") ?: "uk"
        tvCurrentContentLanguage.text = when (langCode) {
            "uk" -> getString(R.string.lang_uk)
            "ru" -> getString(R.string.lang_ru)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_uk)
        }
    }

    private fun showContentLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_uk), getString(R.string.lang_ru), getString(R.string.lang_en))
        val codes = arrayOf("uk", "ru", "en")
        
        val currentCode = prefs.getString("pref_tmdb_lang", "uk")
        val checkedItem = codes.indexOf(currentCode).takeIf { it != -1 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.content_region))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                if (selectedCode != currentCode) {
                    prefs.edit { putString("pref_tmdb_lang", selectedCode) }
                    updateContentLanguageText()
                    Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateThemeText() {
        val themeCode = prefs.getString("pref_theme", "system") ?: "system"
        tvCurrentTheme.text = when (themeCode) {
            "system" -> getString(R.string.theme_system)
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        val codes = arrayOf("system", "light", "dark")
        
        val currentCode = prefs.getString("pref_theme", "system")
        val checkedItem = codes.indexOf(currentCode).takeIf { it != -1 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.theme_title))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                if (selectedCode != currentCode) {
                    prefs.edit { putString("pref_theme", selectedCode) }
                    updateThemeText()
                    applyTheme(selectedCode)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun clearCache() {
        // Simple confirmation
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.clear_cache))
            .setMessage(getString(R.string.clear_cache) + "?")
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                viewModel.clearCache()
                updateCacheSize()
                Toast.makeText(requireContext(), getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateCacheSize() {
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
