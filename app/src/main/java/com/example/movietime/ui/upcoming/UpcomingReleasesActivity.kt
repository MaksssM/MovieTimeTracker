package com.example.movietime.ui.upcoming

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R

class UpcomingReleasesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_placeholder)

        supportActionBar?.title = "Майбутні прем'єри"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO: Implement upcoming releases functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
