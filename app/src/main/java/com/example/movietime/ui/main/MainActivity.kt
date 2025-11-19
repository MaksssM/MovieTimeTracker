package com.example.movietime.ui.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.movietime.R
import com.example.movietime.databinding.DrawerlayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: DrawerlayoutBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "en") ?: "en"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DrawerlayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.watchedFragment, R.id.trendingFragment, R.id.settingsFragment
            ), binding.drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)


        binding.navView.setupWithNavController(navController)
    }
}