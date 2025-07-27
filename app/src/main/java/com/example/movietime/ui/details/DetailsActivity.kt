package com.example.movietime.ui.details

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.movietime.R
import com.example.movietime.databinding.ActivityDetailsBinding
import com.example.movietime.ui.ViewModelFactory
import com.example.movietime.util.loadImage

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var viewModel: DetailsViewModel

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_POSTER_PATH = "extra_poster_path"
        const val EXTRA_OVERVIEW = "extra_overview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = ViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory).get(DetailsViewModel::class.java)

        val id = intent.getIntExtra(EXTRA_ID, -1)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val posterPath = intent.getStringExtra(EXTRA_POSTER_PATH)
        val overview = intent.getStringExtra(EXTRA_OVERVIEW)

        if (id == -1 || type.isNullOrEmpty()) {
            finish()
            return
        }

        viewModel.setMediaId(id, type)
        setupUI(title, posterPath, overview)
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            viewModel.toggleWatchedStatus(title ?: "Без названия", posterPath)
        }
    }

    private fun setupUI(title: String?, posterPath: String?, overview: String?) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbarLayout.title = title
        binding.ivPoster.loadImage(posterPath)
        binding.tvOverview.text = overview
    }

    private fun observeViewModel() {
        viewModel.isWatched.observe(this) { isWatched ->
            if (isWatched) {
                binding.fabAdd.text = "Удалить из просмотренных"
                binding.fabAdd.icon =
                    ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete)
            } else {
                binding.fabAdd.text = "Добавить в просмотренные"
                binding.fabAdd.icon =
                    ContextCompat.getDrawable(this, android.R.drawable.ic_input_add)
            }
        }
    }
}