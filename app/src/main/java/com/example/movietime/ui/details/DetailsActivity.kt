package com.example.movietime.ui.details

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.movietime.databinding.ActivityDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.util.Utils
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private val viewModel: DetailsViewModel by viewModels()

    companion object {
        private const val TAG = "DetailsActivity"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "uk") ?: "uk"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("uk")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Налаштування shared element transition
        window.requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS)
        val slide = Slide(Gravity.BOTTOM).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }
        window.enterTransition = slide
        window.exitTransition = slide
        
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Підтримка postpone для завантаження зображення
        supportPostponeEnterTransition()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val itemId = intent.getIntExtra("ITEM_ID", -1)
        val mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "movie"
        
        if (itemId != -1) {
            when (mediaType) {
                "movie" -> viewModel.loadMovie(itemId)
                "tv" -> viewModel.loadTvShow(itemId)
            }
        }

        observeViewModel()
        setupCategoryButtons()
        setupActionButtons()
        animateEntrance()
    }

    private fun setupActionButtons() {
        // Share button
        binding.btnShare.setOnClickListener {
            shareContent()
        }
        
        // Rate button
        binding.btnRate.setOnClickListener {
            showRatingDialog { rating ->
                if (rating != null) {
                    Toast.makeText(this, getString(R.string.rated_toast, rating.toInt()), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareContent() {
        val item = viewModel.item.value ?: return
        val (title, year) = when (item) {
            is com.example.movietime.data.model.MovieResult -> {
                Pair(item.title ?: "", item.releaseDate?.take(4) ?: "")
            }
            is com.example.movietime.data.model.TvShowResult -> {
                Pair(item.name ?: "", item.firstAirDate?.take(4) ?: "")
            }
            else -> return
        }
        
        val shareText = getString(R.string.share_text, title, year)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }

    private fun animateEntrance() {
        // Hide elements initially
        val elementsToAnimate = listOf(
            binding.btnPlanned,
            binding.btnWatched,
            binding.btnWatching
        )
        
        elementsToAnimate.forEach { view ->
            view.alpha = 0f
            view.translationY = 40f
            view.scaleX = 0.9f
            view.scaleY = 0.9f
        }

        // Staggered animation for action buttons
        lifecycleScope.launch {
            delay(200)
            elementsToAnimate.forEachIndexed { index, view ->
                delay(80L * index)
                animateViewEntrance(view)
            }
        }
    }

    private fun animateViewEntrance(view: View) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 40f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f)
            )
            duration = 400
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    private fun setupCategoryButtons() {
        // Кнопка "Заплановані"
        binding.btnPlanned.setOnClickListener { view ->
            animateButtonPress(view) { addToCategory("planned") }
        }

        // Кнопка "Переглянуті"
        binding.btnWatched.setOnClickListener { view ->
            animateButtonPress(view) { addToCategory("watched") }
        }

        // Кнопка "Дивлюсь"
        binding.btnWatching.setOnClickListener { view ->
            animateButtonPress(view) { addToCategory("watching") }
        }
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.93f, 1.02f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.93f, 1.02f, 1f)
            )
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        view.postDelayed(action, 150)
    }

    private fun addToCategory(category: String) {
        val current = viewModel.item.value
        if (current == null) {
            Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
            return
        }

        val id: Int
        val title: String?
        val posterPath: String?
        val releaseDate: String?
        val runtimeFromApi: Int?
        val mType: String

        when (current) {
            is com.example.movietime.data.model.MovieResult -> {
                id = current.id
                title = current.title
                posterPath = current.posterPath
                releaseDate = current.releaseDate
                runtimeFromApi = current.runtime
                mType = "movie"
            }
            is com.example.movietime.data.model.TvShowResult -> {
                id = current.id
                title = current.name ?: ""
                posterPath = current.posterPath
                releaseDate = current.firstAirDate
                runtimeFromApi = current.episodeRunTime?.firstOrNull() ?: 0
                mType = "tv"
            }
            else -> {
                Toast.makeText(this, getString(R.string.unknown_media), Toast.LENGTH_SHORT).show()
                return
            }
        }

        when (category) {
            "watched" -> addToWatched(id, title, posterPath, releaseDate, runtimeFromApi, mType, current)
            "planned" -> addToPlanned(id, title, posterPath, releaseDate, runtimeFromApi, mType, current)
            "watching" -> addToWatching(id, title, posterPath, releaseDate, runtimeFromApi, mType, current)
        }
    }

    private fun addToWatched(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String, current: Any) {
        viewModel.isItemWatched(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_watched), Toast.LENGTH_SHORT).show()
                }
            } else {
                when (current) {
                    is com.example.movietime.data.model.MovieResult -> {
                        // Для фільмів все працює як раніше
                        val watched = Utils.createWatchedItemFromMovie(
                            id = id,
                            title = title,
                            name = null,
                            posterPath = posterPath,
                            releaseDate = releaseDate,
                            runtime = runtimeFromApi,
                            mediaType = mType,
                            overview = current.overview,
                            voteAverage = current.voteAverage?.toDouble(),
                            userRating = null
                        )

                        viewModel.addWatchedItem(watched) { success ->
                            runOnUiThread {
                                if (success) {
                                    disableButton(binding.btnWatched)
                                    Toast.makeText(this, getString(R.string.added_to_watched_toast), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    is com.example.movietime.data.model.TvShowResult -> {
                        // Для серіалів використовуємо спеціальний метод з розрахунком
                        val runtimeInfo = Utils.autoComputeTvShowRuntime(current)
                        val watched = WatchedItem(
                            id = id,
                            title = title ?: "",
                            posterPath = posterPath,
                            releaseDate = releaseDate,
                            runtime = runtimeInfo.totalMinutes,
                            mediaType = mType,
                            overview = current.overview,
                            voteAverage = current.voteAverage?.toDouble(),
                            episodeRuntime = runtimeInfo.episodeRuntime,
                            totalEpisodes = runtimeInfo.episodes,
                            isOngoing = runtimeInfo.isOngoing,
                            status = current.status
                        )

                        viewModel.addWatchedItem(watched) { success ->
                            runOnUiThread {
                                if (success) {
                                    disableButton(binding.btnWatched)
                                    Toast.makeText(this, getString(R.string.added_to_watched_toast), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addToPlanned(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String, current: Any) {
        viewModel.isItemPlanned(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_planned), Toast.LENGTH_SHORT).show()
                }
            } else {
                val planned = if (current is com.example.movietime.data.model.TvShowResult) {
                    val runtimeInfo = Utils.autoComputeTvShowRuntime(current)
                    WatchedItem(
                        id = id,
                        title = title ?: "",
                        posterPath = posterPath,
                        releaseDate = releaseDate,
                        runtime = runtimeInfo.totalMinutes,
                        mediaType = mType,
                        overview = null,
                        voteAverage = null,
                        episodeRuntime = runtimeInfo.episodeRuntime,
                        totalEpisodes = runtimeInfo.episodes,
                        isOngoing = runtimeInfo.isOngoing
                    )
                } else {
                    Utils.createWatchedItemFromMovie(
                        id = id,
                        title = title,
                        name = null,
                        posterPath = posterPath,
                        releaseDate = releaseDate,
                        runtime = runtimeFromApi,
                        mediaType = mType,
                        overview = null,
                        voteAverage = null,
                        userRating = null
                    )
                }

                viewModel.addToPlanned(planned) { success ->
                    runOnUiThread {
                        if (success) {
                            disableButton(binding.btnPlanned)
                            Toast.makeText(this, getString(R.string.added_to_planned), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun addToWatching(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String, current: Any) {
        viewModel.isItemWatching(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_watching), Toast.LENGTH_SHORT).show()
                }
            } else {
                val watching = if (current is com.example.movietime.data.model.TvShowResult) {
                    val runtimeInfo = Utils.autoComputeTvShowRuntime(current)
                    WatchedItem(
                        id = id,
                        title = title ?: "",
                        posterPath = posterPath,
                        releaseDate = releaseDate,
                        runtime = runtimeInfo.totalMinutes,
                        mediaType = mType,
                        overview = null,
                        voteAverage = null,
                        episodeRuntime = runtimeInfo.episodeRuntime,
                        totalEpisodes = runtimeInfo.episodes,
                        isOngoing = runtimeInfo.isOngoing
                    )
                } else {
                    Utils.createWatchedItemFromMovie(
                        id = id,
                        title = title,
                        name = null,
                        posterPath = posterPath,
                        releaseDate = releaseDate,
                        runtime = runtimeFromApi,
                        mediaType = mType,
                        overview = null,
                        voteAverage = null,
                        userRating = null
                    )
                }

                viewModel.addToWatching(watching) { success ->
                    runOnUiThread {
                        if (success) {
                            disableButton(binding.btnWatching)
                            Toast.makeText(this, getString(R.string.added_to_watching), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun disableButton(view: android.view.View) {
        view.alpha = 0.5f
        view.isClickable = false
        view.isFocusable = false
    }

    private fun showRatingDialog(onRatingSelected: (Float?) -> Unit) {
        val ratings = arrayOf("Пропустити", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rate_content))
            .setItems(ratings) { _, which ->
                val userRating = if (which == 0) null else which.toFloat()
                onRatingSelected(userRating)
            }
            .setOnCancelListener {
                onRatingSelected(null)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.item.observe(this) { item ->
            Log.d(TAG, "ViewModel item updated: $item")
            when (item) {
                is com.example.movietime.data.model.MovieResult -> {
                    Log.d(TAG, "Displaying movie: ${item.title}, runtime=${item.runtime}")
                    binding.toolbarLayout.title = item.title ?: getString(R.string.unknown_media)
                    binding.tvTitle.text = item.title ?: getString(R.string.unknown_media)
                    binding.tvOverview.text = item.overview ?: getString(R.string.no_description_available)

                    // Year
                    binding.tvYear.text = item.releaseDate?.take(4) ?: ""

                    // Rating
                    val rating = item.voteAverage ?: 0f
                    binding.tvRating.text = String.format("%.1f", rating)
                    val ratingPercent = (rating * 10).toInt()
                    binding.tvRatingPercent.text = "$ratingPercent%"
                    binding.progressRating.progress = ratingPercent

                    // Vote count
                    val voteCount = item.voteCount ?: 0
                    binding.tvVoteCount.text = formatVoteCount(voteCount)

                    // Release date
                    binding.tvReleaseDate.text = formatShortDate(item.releaseDate)

                    // Display runtime
                    val runtime = item.runtime ?: 0
                    binding.tvRuntime.text = if (runtime > 0) {
                        val hours = runtime / 60
                        val minutes = runtime % 60
                        if (hours > 0 && minutes > 0) {
                            "${hours}h ${minutes}m"
                        } else if (hours > 0) {
                            "${hours}h"
                        } else {
                            "${minutes}m"
                        }
                    } else {
                        "N/A"
                    }

                    // Genres chips
                    setupGenreChips(getGenreNames(item.genreIds, isMovie = true))

                    // Use backdrop for header, fallback to poster
                    Log.d(TAG, "Movie backdrop: ${item.backdropPath}, poster: ${item.posterPath}")
                    val imageUrl = when {
                        !item.backdropPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w1280${item.backdropPath}"
                        !item.posterPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w780${item.posterPath}"
                        else -> null
                    }
                    Log.d(TAG, "Loading image URL: $imageUrl")
                    
                    if (imageUrl != null) {
                        binding.ivPoster.load(imageUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_placeholder)
                            error(R.drawable.ic_placeholder)
                            listener(
                                onSuccess = { _, _ ->
                                    Log.d(TAG, "Image loaded successfully")
                                    supportStartPostponedEnterTransition()
                                },
                                onError = { _, throwable ->
                                    Log.e(TAG, "Image load failed: ${throwable.throwable?.message}")
                                    supportStartPostponedEnterTransition()
                                }
                            )
                        }
                    } else {
                        Log.w(TAG, "No image URL available")
                        binding.ivPoster.setImageResource(R.drawable.ic_placeholder)
                        supportStartPostponedEnterTransition()
                    }
                }
                is com.example.movietime.data.model.TvShowResult -> {
                    binding.toolbarLayout.title = item.name ?: getString(R.string.unknown_media)
                    binding.tvTitle.text = item.name ?: getString(R.string.unknown_media)
                    binding.tvOverview.text = item.overview ?: getString(R.string.no_description_available)

                    // Year
                    binding.tvYear.text = item.firstAirDate?.take(4) ?: ""

                    // Rating
                    val rating = item.voteAverage ?: 0f
                    binding.tvRating.text = String.format("%.1f", rating)
                    val ratingPercent = (rating * 10).toInt()
                    binding.tvRatingPercent.text = "$ratingPercent%"
                    binding.progressRating.progress = ratingPercent

                    // Vote count
                    val voteCount = item.voteCount ?: 0
                    binding.tvVoteCount.text = formatVoteCount(voteCount)

                    // Release date
                    binding.tvReleaseDate.text = formatShortDate(item.firstAirDate)

                    // Display episode runtime
                    val episodeRuntime = item.episodeRunTime?.firstOrNull() ?: 0
                    binding.tvRuntime.text = if (episodeRuntime > 0) {
                        "${episodeRuntime}m/ep"
                    } else {
                        "N/A"
                    }

                    // Genres chips
                    setupGenreChips(getGenreNames(item.genreIds, isMovie = false))

                    // Use backdrop for header, fallback to poster
                    Log.d(TAG, "TV backdrop: ${item.backdropPath}, poster: ${item.posterPath}")
                    val imageUrl = when {
                        !item.backdropPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w1280${item.backdropPath}"
                        !item.posterPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w780${item.posterPath}"
                        else -> null
                    }
                    Log.d(TAG, "Loading TV image URL: $imageUrl")
                    
                    if (imageUrl != null) {
                        binding.ivPoster.load(imageUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_placeholder)
                            error(R.drawable.ic_placeholder)
                            listener(
                                onSuccess = { _, _ ->
                                    Log.d(TAG, "TV Image loaded successfully")
                                    supportStartPostponedEnterTransition()
                                },
                                onError = { _, throwable ->
                                    Log.e(TAG, "TV Image load failed: ${throwable.throwable?.message}")
                                    supportStartPostponedEnterTransition()
                                }
                            )
                        }
                    } else {
                        Log.w(TAG, "No TV image URL available")
                        binding.ivPoster.setImageResource(R.drawable.ic_placeholder)
                        supportStartPostponedEnterTransition()
                    }
                }
                null -> {
                    Log.w(TAG, "Item is null - failed to load data")
                    binding.toolbarLayout.title = getString(R.string.error_loading_data)
                    binding.tvOverview.text = getString(R.string.no_description_available)
                    binding.tvRuntime.text = "N/A"
                }
                else -> {
                    Log.w(TAG, "Unknown item type: ${item?.javaClass?.simpleName}")
                }
            }
        }
    }

    private fun getGenreNames(genreIds: List<Int>, isMovie: Boolean): List<String> {
        // TMDB genre mappings
        val movieGenres = mapOf(
            28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
            80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
            14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
            9648 to "Mystery", 10749 to "Romance", 878 to "Sci-Fi", 10770 to "TV Movie",
            53 to "Thriller", 10752 to "War", 37 to "Western"
        )
        val tvGenres = mapOf(
            10759 to "Action", 16 to "Animation", 35 to "Comedy", 80 to "Crime",
            99 to "Documentary", 18 to "Drama", 10751 to "Family", 10762 to "Kids",
            9648 to "Mystery", 10763 to "News", 10764 to "Reality", 10765 to "Sci-Fi",
            10766 to "Soap", 10767 to "Talk", 10768 to "War & Politics", 37 to "Western"
        )
        val genres = if (isMovie) movieGenres else tvGenres
        return genreIds.mapNotNull { genres[it] }
    }

    private fun setupGenreChips(genres: List<String?>) {
        binding.chipGroupGenres.removeAllViews()
        genres.filterNotNull().take(4).forEach { genre ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = genre
                isClickable = false
                setChipBackgroundColorResource(R.color.chip_genre_bg)
                setTextColor(resources.getColor(R.color.chip_genre_text, theme))
                chipStrokeWidth = 0f
                textSize = 12f
                chipMinHeight = 32f.dpToPx()
                chipCornerRadius = 16f.dpToPx()
            }
            binding.chipGroupGenres.addView(chip)
        }
    }

    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    private fun formatVoteCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    private fun formatShortDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM d", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: "N/A"
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }
    
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.activity_close_exit)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}