package com.example.movietime.ui.details

import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.movietime.databinding.ActivityTvDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.util.Utils
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.R

@AndroidEntryPoint
class TvDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvDetailsBinding
    private val viewModel: TvDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvId = intent.getIntExtra("TV_ID", -1)

        if (tvId != -1) {
            viewModel.loadTvShow(tvId)
        }

        observeViewModel()

        // Обробник додавання в переглянуті
        binding.fabAdd.setOnClickListener {
            val current = viewModel.tvShow.value
            if (current == null) {
                Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = current.id
            val title = current.name ?: ""
            val posterPath = current.posterPath
            val releaseDate = current.firstAirDate
            val episodeRuntime = current.episodeRunTime?.firstOrNull() ?: 0

            // Перевіримо, чи вже є в БД
            viewModel.isItemWatched(id, "tv") { exists ->
                if (exists) {
                    runOnUiThread {
                        binding.fabAdd.text = getString(R.string.already_watched)
                        Toast.makeText(this, getString(R.string.already_watched), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Simplified TV show adding - seasons feature to be implemented later
                    runOnUiThread {
                        // For now, just add TV show without season/episode details
                        val item = WatchedItem(
                            id = id,
                            title = title,
                            posterPath = posterPath,
                            releaseDate = releaseDate,
                            runtime = episodeRuntime,
                            mediaType = "tv"
                        )

                        viewModel.addWatchedItem(item) { success ->
                            runOnUiThread {
                                if (success) {
                                    binding.fabAdd.text = getString(R.string.added)
                                    Toast.makeText(this@TvDetailsActivity, getString(R.string.added_to_watched_toast), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@TvDetailsActivity, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.tvShow.observe(this) { tvShow ->
            if (tvShow != null) {
                binding.toolbarLayout.title = tvShow.name ?: getString(R.string.unknown_media)
                binding.tvOverview.text = tvShow.overview
                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.load(poster)

                // Показуємо кількість сезонів та епізодів (simplified)
                val seasonCount = tvShow.numberOfSeasons ?: 0
                binding.tvSeasonCount.text = seasonCount.toString()

                val totalEpisodes = tvShow.numberOfEpisodes ?: 0
                binding.tvTotalEpisodes.text = totalEpisodes.toString()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}