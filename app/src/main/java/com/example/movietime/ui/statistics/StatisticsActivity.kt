package com.example.movietime.ui.statistics

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.ActivityStatisticsBinding
import com.example.movietime.ui.person.PersonDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var genreAdapter: GenreStatAdapter
    private lateinit var directorAdapter: DirectorStatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdapters()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupAdapters() {
        genreAdapter = GenreStatAdapter()
        binding.rvGenres.apply {
            adapter = genreAdapter
            layoutManager = LinearLayoutManager(this@StatisticsActivity)
            setHasFixedSize(false)
        }

        directorAdapter = DirectorStatAdapter { director ->
            // Open person details
            val intent = Intent(this, PersonDetailsActivity::class.java).apply {
                putExtra("PERSON_ID", director.directorId)
                putExtra("PERSON_NAME", director.directorName)
            }
            startActivity(intent)
        }
        binding.rvDirectors.apply {
            adapter = directorAdapter
            layoutManager = LinearLayoutManager(this@StatisticsActivity)
            setHasFixedSize(false)
        }
    }

    private fun observeViewModel() {
        viewModel.statistics.observe(this) { stats ->
            updateUI(stats)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.isVisible = isLoading
        }

        viewModel.directorsLoading.observe(this) { isLoading ->
            binding.progressDirectors.isVisible = isLoading
        }

        viewModel.error.observe(this) { error ->
            // Could show error UI here
        }
    }

    private fun updateUI(stats: com.example.movietime.data.model.DetailedStatistics) {
        // Total watch time
        binding.tvTotalWatchTime.text = viewModel.formatWatchTime(stats.totalWatchTimeMinutes)
        
        val days = stats.totalWatchTimeMinutes / 1440.0
        binding.tvWatchTimeEquivalent.text = if (days >= 1) {
            getString(R.string.watch_time_equivalent_days, days)
        } else {
            val hours = stats.totalWatchTimeMinutes / 60.0
            getString(R.string.watch_time_equivalent_hours, hours)
        }

        // Content counts
        binding.tvTotalMovies.text = stats.totalMovies.toString()
        binding.tvTotalTvShows.text = stats.totalTvShows.toString()
        binding.tvTotalEpisodes.text = stats.totalTvEpisodes.toString()

        // Average ratings
        binding.tvAvgMovieRating.text = String.format(Locale.US, "%.1f", stats.averageMovieRating)
        binding.tvAvgTvRating.text = String.format(Locale.US, "%.1f", stats.averageTvRating)

        // Favorite genres
        if (stats.favoriteGenres.isNotEmpty()) {
            binding.rvGenres.isVisible = true
            binding.tvNoGenres.isVisible = false
            genreAdapter.submitList(stats.favoriteGenres)
        } else {
            binding.rvGenres.isVisible = false
            binding.tvNoGenres.isVisible = true
        }

        // Favorite directors
        if (stats.favoriteDirectors.isNotEmpty()) {
            binding.rvDirectors.isVisible = true
            binding.tvNoDirectors.isVisible = false
            directorAdapter.submitList(stats.favoriteDirectors)
        } else {
            binding.rvDirectors.isVisible = false
            binding.tvNoDirectors.isVisible = true
        }

        // Longest movie
        stats.longestMovieWatched?.let { movie ->
            binding.cardLongestMovie.isVisible = true
            binding.tvLongestMovieTitle.text = movie.title
            binding.tvLongestMovieRuntime.text = getString(R.string.runtime_minutes, movie.runtimeMinutes)
        } ?: run {
            binding.cardLongestMovie.isVisible = false
        }

        // Most rewatched
        stats.mostRewatchedItem?.let { item ->
            binding.cardMostRewatched.isVisible = true
            binding.tvMostRewatchedTitle.text = item.title
            binding.tvRewatchCount.text = resources.getQuantityString(
                R.plurals.times_count, item.rewatchCount, item.rewatchCount
            )
        } ?: run {
            binding.cardMostRewatched.isVisible = false
        }

        // Content ratio
        val moviePercent = (stats.movieVsTvRatio * 100).toInt()
        val tvPercent = 100 - moviePercent
        binding.progressRatio.progress = moviePercent
        binding.tvMoviePercent.text = "🎬 $moviePercent% ${getString(R.string.movies)}"
        binding.tvTvPercent.text = "📺 $tvPercent% ${getString(R.string.tv_shows)}"
    }
}
