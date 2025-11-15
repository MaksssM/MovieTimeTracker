package com.example.movietime.ui.friends

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movietime.R

class FriendsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_placeholder)

        supportActionBar?.title = "Друзі"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO: Implement friends functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
