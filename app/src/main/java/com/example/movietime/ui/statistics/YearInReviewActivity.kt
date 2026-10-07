package com.example.movietime.ui.statistics

import android.content.Context
import android.os.Bundle
import com.example.movietime.ui.base.BaseActivity
import com.example.movietime.R
import com.example.movietime.databinding.ActivityYearInReviewBinding
import com.example.movietime.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class YearInReviewActivity : BaseActivity() {

    private lateinit var binding: ActivityYearInReviewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYearInReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, YearInReviewFragment.newInstance())
                .commit()
        }
    }
}
