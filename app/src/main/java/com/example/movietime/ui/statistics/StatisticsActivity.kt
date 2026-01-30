package com.example.movietime.ui.statistics

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
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
        prepareAnimations()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun prepareAnimations() {
        // Prepare stat cards for animation (only cards with IDs)
        val cards = listOf(
            binding.cardWatchTime,
            binding.cardLongestMovie,
            binding.cardMostRewatched,
            binding.cardTrends
        )
        
        cards.forEach { card ->
            card.alpha = 0f
            card.translationY = 50f
        }
    }
    
    private fun animateStatsAppearance() {
        val cards = listOf(
            binding.cardWatchTime,
            binding.cardLongestMovie,
            binding.cardMostRewatched,
            binding.cardTrends
        )
        
        cards.forEachIndexed { index, card ->
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 60).toLong())
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }
    }
    
    private fun animateCounterValue(textView: android.widget.TextView, value: String) {
        textView.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                textView.text = value
                textView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
            .start()
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
        // Trigger card animations
        animateStatsAppearance()
        
        // Total watch time
        binding.tvTotalWatchTime.text = viewModel.formatWatchTime(stats.totalWatchTimeMinutes)
        
        val days = stats.totalWatchTimeMinutes / 1440.0
        binding.tvWatchTimeEquivalent.text = if (days >= 1) {
            getString(R.string.watch_time_equivalent_days, days)
        } else {
            val hours = stats.totalWatchTimeMinutes / 60.0
            getString(R.string.watch_time_equivalent_hours, hours)
        }

        // Content counts with animation
        animateCounterValue(binding.tvTotalMovies, stats.totalMovies.toString())
        animateCounterValue(binding.tvTotalTvShows, stats.totalTvShows.toString())
        animateCounterValue(binding.tvTotalEpisodes, stats.totalTvEpisodes.toString())

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

        // This month stats
        binding.tvThisMonthMovies.text = stats.thisMonthMovies.toString()
        binding.tvThisMonthEpisodes.text = stats.thisMonthEpisodes.toString()
        val thisMonthHours = stats.thisMonthMinutes / 60
        binding.tvThisMonthTime.text = "${thisMonthHours}h"

        // Achievements / Milestones
        binding.tvCurrentStreak.text = stats.currentStreak.toString()

        // Best month
        if (stats.bestMonth != null) {
            binding.tvBestMonth.text = stats.bestMonth.monthName
            binding.tvBestMonthCount.text = resources.getQuantityString(
                R.plurals.movies_count, stats.bestMonth.count, stats.bestMonth.count
            )
        } else {
            binding.tvBestMonth.text = "—"
            binding.tvBestMonthCount.text = ""
        }
        
        // Enhanced stats - viewing trends
        binding.tvAvgDailyTime.text = viewModel.formatWatchTimeShort(stats.avgDailyWatchMinutes)
        binding.tvUniqueGenres.text = stats.totalUniqueGenres.toString()
        binding.tvCompletedShows.text = stats.completedTvShows.toString()
        
        // First watch date
        stats.firstWatchDate?.let { timestamp ->
            binding.layoutFirstWatch.isVisible = true
            val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            binding.tvFirstWatchDate.text = dateFormat.format(java.util.Date(timestamp))
        } ?: run {
            binding.layoutFirstWatch.isVisible = false
        }
    }
}
