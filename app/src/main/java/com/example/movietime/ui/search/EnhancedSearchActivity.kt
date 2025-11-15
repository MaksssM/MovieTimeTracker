package com.example.movietime.ui.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R

class EnhancedSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For now, just show a placeholder message
        setContentView(R.layout.activity_placeholder)

        supportActionBar?.title = "Покращений пошук"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO: Implement enhanced search functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
