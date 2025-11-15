package com.example.movietime.ui.planned

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R

class PlannedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_placeholder)

        val isMovie = intent.getBooleanExtra("isMovie", true)
        val title = if (isMovie) "Заплановані фільми" else "Заплановані серіали"

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO: Implement planned content functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
