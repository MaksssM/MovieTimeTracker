package com.example.movietime.ui.upcoming

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R
import java.util.Locale

class UpcomingReleasesActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_placeholder)

        supportActionBar?.title = getString(R.string.movie_premieres)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup back button click handler
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // TODO: Implement upcoming releases functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
