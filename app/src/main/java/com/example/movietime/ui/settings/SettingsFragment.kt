package com.example.movietime.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.movietime.R
import java.util.Locale

class SettingsFragment : Fragment() {

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
        val themePref = prefs.getString("pref_theme", "system") ?: "system"
        val langPref = prefs.getString("pref_lang", "en") ?: "en"

        val radioThemeGroup = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioLangGroup = view.findViewById<RadioGroup>(R.id.radioGroupLanguage)

        // Set initial checked state for theme
        when (themePref) {
            "system" -> radioThemeGroup.check(R.id.radioThemeSystem)
            "light" -> radioThemeGroup.check(R.id.radioThemeLight)
            "dark" -> radioThemeGroup.check(R.id.radioThemeDark)
            else -> radioThemeGroup.check(R.id.radioThemeSystem)
        }

        // Set initial checked state for language
        when (langPref) {
            "uk" -> radioLangGroup.check(R.id.radioLangUk)
            "ru" -> radioLangGroup.check(R.id.radioLangRu)
            "en" -> radioLangGroup.check(R.id.radioLangEn)
            else -> radioLangGroup.check(R.id.radioLangEn)
        }

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

        radioLangGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioLangUk -> "uk"
                R.id.radioLangRu -> "ru"
                R.id.radioLangEn -> "en"
                else -> "en"
            }
            prefs.edit { putString("pref_lang", value) }
            applyLocale(value)
            Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            // Перезапустимо Activity, щоб застосувати мову у всьому UI
            requireActivity().recreate()
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
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val activity = requireActivity()
        val res = activity.resources
        val config = Configuration(res.configuration)
        // Set locales (min SDK for this project >= 24 so we can set LocaleList)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        // Create new context with updated locale; actual UI will update on recreate()
        activity.createConfigurationContext(config)
    }
}
