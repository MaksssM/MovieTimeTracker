package com.example.movietime.ui.upcoming

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R
import java.util.Locale

class UpcomingReleasesActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(newBase))
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
