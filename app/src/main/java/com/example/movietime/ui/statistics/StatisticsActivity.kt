package com.example.movietime.ui.statistics

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(newBase))
    }

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
            binding.cardTrends,
            binding.cardExtended,
            binding.cardWatchTimeBreakdown,
            binding.cardHighestRated
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
            binding.cardTrends,
            binding.cardExtended,
            binding.cardWatchTimeBreakdown,
            binding.cardHighestRated
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

        // Runtime records card — show if any of shortest/longest exists
        val hasRuntimeData = stats.longestMovieWatched != null ||
                stats.shortestMovie != null || stats.longestTvShow != null
        binding.cardLongestMovie.isVisible = hasRuntimeData

        stats.longestMovieWatched?.let { movie ->
            binding.tvLongestMovieTitle.isVisible = true
            binding.tvLongestMovieRuntime.isVisible = true
            binding.tvLongestMovieTitle.text = movie.title
            binding.tvLongestMovieRuntime.text = getString(R.string.runtime_minutes, movie.runtimeMinutes)
        } ?: run {
            binding.tvLongestMovieTitle.isVisible = false
            binding.tvLongestMovieRuntime.isVisible = false
        }

        stats.shortestMovie?.let { movie ->
            binding.tvShortestMovieTitle.isVisible = true
            binding.tvShortestMovieRuntime.isVisible = true
            binding.tvShortestMovieTitle.text = movie.title
            binding.tvShortestMovieRuntime.text = getString(R.string.runtime_minutes, movie.runtimeMinutes)
        } ?: run {
            binding.tvShortestMovieTitle.isVisible = false
            binding.tvShortestMovieRuntime.isVisible = false
        }

        stats.longestTvShow?.let { tv ->
            binding.tvLongestTvTitle.isVisible = true
            binding.tvLongestTvRuntime.isVisible = true
            binding.tvLongestTvTitle.text = tv.title
            val tvHours = tv.runtimeMinutes / 60
            val tvMins = tv.runtimeMinutes % 60
            binding.tvLongestTvRuntime.text = if (tvHours > 0) "${tvHours}г ${tvMins}хв" else "${tvMins}хв"
        } ?: run {
            binding.tvLongestTvTitle.isVisible = false
            binding.tvLongestTvRuntime.isVisible = false
        }

        // Most rewatched
        stats.mostRewatchedItem?.let { item ->
            binding.cardMostRewatched.isVisible = true
            binding.tvMostRewatchedTitle.text = item.title
            binding.tvRewatchCount.text = "${item.rewatchCount}×"
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
        
        // Extended statistics
        binding.tvTotalRewatches.text = stats.totalRewatches.toString()
        binding.tvAvgMovieRuntime.text = getString(R.string.runtime_minutes, stats.avgMovieRuntime)
        binding.tvAvgMoviesPerMonth.text = String.format(Locale.US, "%.1f", stats.avgMoviesPerMonth)
        binding.tvAvgContentPerMonth.text = String.format(Locale.US, "%.1f", stats.avgContentPerMonth)
        
        // Most popular genre
        if (stats.mostPopularGenre.isNotEmpty()) {
            binding.layoutMostPopularGenre.isVisible = true
            binding.tvMostPopularGenre.text = stats.mostPopularGenre
        } else {
            binding.layoutMostPopularGenre.isVisible = false
        }
        
        // Watch time breakdown
        binding.tvMovieWatchTime.text = viewModel.formatWatchTimeShort(stats.totalWatchTimeMovies)
        binding.tvTvWatchTime.text = viewModel.formatWatchTimeShort(stats.totalWatchTimeTvShows)
        
        // Highest rated movie
        stats.highestRatedMovie?.let { movie ->
            binding.cardHighestRated.isVisible = true
            binding.tvHighestRatedTitle.text = movie.title
            binding.tvHighestRatedScore.text = String.format(Locale.US, "%.1f ⭐", movie.userRating)
        } ?: run {
            binding.cardHighestRated.isVisible = false
        }
    }
}
