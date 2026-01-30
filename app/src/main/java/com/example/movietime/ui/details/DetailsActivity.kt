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
import coil.request.CachePolicy
import com.example.movietime.databinding.ActivityDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.util.Utils
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.TextView
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
        observeWatchedStatus()
    }

    private fun observeWatchedStatus() {
        viewModel.watchedItem.observe(this) { watchedItem ->
            // Hide all badges first
            binding.cardWatchedBadge.visibility = View.GONE
            binding.cardPlannedBadge.visibility = View.GONE
            binding.cardWatchingBadge.visibility = View.GONE
            binding.layoutWatchCount.visibility = View.GONE

            if (watchedItem != null) {
                // Show watched badge with enhanced info and animation
                animateBadgeAppear(binding.cardWatchedBadge)
                
                // Show user rating if available
                val userRating = watchedItem.userRating
                if (userRating != null && userRating > 0) {
                    binding.tvWatchedBadge.text = getString(R.string.watched) + " ⭐ ${userRating.toInt()}"
                } else {
                    binding.tvWatchedBadge.text = getString(R.string.watched)
                }

                // Disable watched button
                disableButton(binding.btnWatched)

                if (watchedItem.watchCount > 1) {
                    binding.layoutWatchCount.visibility = View.VISIBLE
                    binding.tvWatchCount.text = getString(R.string.watched_times, watchedItem.watchCount)
                }
                
                // Also disable other buttons since it's in watched
                disableButton(binding.btnPlanned)
                disableButton(binding.btnWatching)
            } else {
                // Check planned status
                val itemId = intent.getIntExtra("ITEM_ID", -1)
                val mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "movie"

                viewModel.isItemPlanned(itemId, mediaType) { isPlanned ->
                    runOnUiThread {
                        if (isPlanned) {
                            animateBadgeAppear(binding.cardPlannedBadge)
                            disableButton(binding.btnPlanned)
                        } else {
                            // Check watching status
                            viewModel.isItemWatching(itemId, mediaType) { isWatching ->
                                runOnUiThread {
                                    if (isWatching) {
                                        animateBadgeAppear(binding.cardWatchingBadge)
                                        disableButton(binding.btnWatching)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupActionButtons() {
        // Share button
        binding.btnShare.setOnClickListener {
            shareContent()
        }
        
        // Rate button - now properly saves rating
    binding.btnRate.setOnClickListener {
        com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
        val itemId = intent.getIntExtra("ITEM_ID", -1)
        val mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "movie"
        
        showRatingDialog { rating ->
            if (rating != null) {
                com.example.movietime.utils.HapticFeedbackHelper.impactMedium(it)
                viewModel.updateUserRating(itemId, mediaType, rating) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, getString(R.string.rated_toast, rating.toInt()), Toast.LENGTH_SHORT).show()
                        } else {
                            // Item not in watched list yet - show hint
                            Toast.makeText(this, "Спочатку додайте до переглянутих", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

        // Add to Collection button
        binding.btnAddToCollection.setOnClickListener {
            val item = viewModel.item.value ?: return@setOnClickListener
            val (id, title, posterPath) = when (item) {
                is com.example.movietime.data.model.MovieResult -> {
                    Triple(item.id, item.title, item.posterPath)
                }
                is com.example.movietime.data.model.TvShowResult -> {
                    Triple(item.id, item.name, item.posterPath)
                }
                else -> return@setOnClickListener
            }

            val bottomSheet = com.example.movietime.ui.collections.AddToCollectionBottomSheet.newInstance(
                itemId = id,
                mediaType = if (item is com.example.movietime.data.model.TvShowResult) "tv" else "movie",
                title = title,
                posterPath = posterPath
            )
            bottomSheet.show(supportFragmentManager, "AddToCollectionBottomSheet")
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
            binding.btnPlanned,
            binding.btnWatched,
            binding.btnWatching,
            binding.btnAddToCollection
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

    private fun animateBadgeAppear(badge: View) {
        badge.visibility = View.VISIBLE
        badge.alpha = 0f
        badge.scaleX = 0f
        badge.scaleY = 0f
        
        badge.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
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
        com.example.movietime.utils.HapticFeedbackHelper.impactLow(view)
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
                action()
            }
            .start()
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
                            userRating = null,
                            genreIds = current.genreIds
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
                        // Для серіалів відразу відкриваємо діалог вибору епізодів
                        runOnUiThread {
                            val bottomSheet = TvProgressBottomSheet.newInstance(id) { watchedRuntime ->
                                // Callback коли прогрес збережено
                                if (watchedRuntime > 0) {
                                    disableButton(binding.btnWatched)
                                    Toast.makeText(this, getString(R.string.added_to_watched_toast), Toast.LENGTH_SHORT).show()
                                }
                            }
                            bottomSheet.show(supportFragmentManager, "TvProgressBottomSheet")
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
                        isOngoing = runtimeInfo.isOngoing,
                        genreIds = current.genreIds?.joinToString(",")
                    )
                } else if (current is com.example.movietime.data.model.MovieResult) {
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
                        userRating = null,
                        genreIds = current.genreIds
                    )
                } else {
                     // Fallback should not be reached due to types
                     Utils.createWatchedItemFromMovie(id, title, null, posterPath, releaseDate, runtimeFromApi, mType)
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
                        isOngoing = runtimeInfo.isOngoing,
                        genreIds = current.genreIds?.joinToString(",")
                    )
                } else if (current is com.example.movietime.data.model.MovieResult) {
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
                        userRating = null,
                        genreIds = current.genreIds
                    )
                } else {
                    // Fallback
                    Utils.createWatchedItemFromMovie(id, title, null, posterPath, releaseDate, runtimeFromApi, mType)
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
                    // Title only in the floating card, not in toolbar
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

                    // Genres chips - use genres from details API, fallback to genre_ids from list API
                    val genreNames = if (!item.genres.isNullOrEmpty()) {
                        item.genres.mapNotNull { it.name }
                    } else {
                        getGenreNames(item.genreIds ?: emptyList(), isMovie = true)
                    }
                    setupGenreChips(genreNames)


                    // Use backdrop for header, fallback to poster
                    Log.d(TAG, "Movie backdrop: '${item.backdropPath}', poster: '${item.posterPath}'")
                    // First try poster for movie, then backdrop as fallback
                    val imageUrl = when {
                        !item.posterPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w780${item.posterPath}"
                        !item.backdropPath.isNullOrEmpty() -> "https://image.tmdb.org/t/p/w780${item.backdropPath}"
                        else -> null
                    }
                    Log.d(TAG, "Loading movie image URL: $imageUrl")
                    
                    if (imageUrl != null) {
                        binding.ivPoster.load(imageUrl) {
                            crossfade(true)
                            allowHardware(false)
                            size(780, 1170)
                            placeholder(R.drawable.ic_placeholder)
                            error(R.drawable.ic_placeholder)
                            listener(
                                onStart = { request ->
                                    Log.d(TAG, "Started loading image: ${request.data}")
                                },
                                onSuccess = { _, result ->
                                    Log.d(TAG, "Image loaded successfully from: ${result.dataSource}")
                                    supportStartPostponedEnterTransition()
                                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                    if (bitmap != null) {
                                        applyDynamicColors(bitmap)
                                    }
                                },
                                onError = { request, throwable ->
                                    Log.e(TAG, "Image load failed for ${request.data}: ${throwable.throwable?.message}", throwable.throwable)
                                    supportStartPostponedEnterTransition()
                                }
                            )
                        }
                    } else {
                        Log.w(TAG, "No image URL available for movie")
                        binding.ivPoster.setImageResource(R.drawable.ic_placeholder)
                        supportStartPostponedEnterTransition()
                    }

                    // Collection Info
                    if (item.belongsToCollection != null) {
                        binding.layoutCollection.visibility = View.VISIBLE
                        binding.tvCollectionName.text = item.belongsToCollection.name
                        
                        // Load collection poster if available
                        val collectionPoster = item.belongsToCollection.posterPath
                        if (!collectionPoster.isNullOrEmpty()) {
                            binding.ivCollectionPoster.load("https://image.tmdb.org/t/p/w342$collectionPoster") {
                                crossfade(true)
                                placeholder(R.color.poster_placeholder_dark)
                            }
                        }

                        binding.btnCollection.setOnClickListener {
                            val intent = android.content.Intent(this, com.example.movietime.ui.collection.CollectionDetailsActivity::class.java).apply {
                                putExtra("COLLECTION_ID", item.belongsToCollection.id)
                            }
                            startActivity(intent)
                        }
                    } else {
                        binding.layoutCollection.visibility = View.GONE
                    }
                }
                is com.example.movietime.data.model.TvShowResult -> {
                    // Title only in the floating card
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
                    val tvGenreNames = if (!item.genres.isNullOrEmpty()) {
                        item.genres.mapNotNull { it.name }
                    } else {
                        getGenreNames(item.genreIds ?: emptyList(), isMovie = false)
                    }
                    setupGenreChips(tvGenreNames)


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
                            allowHardware(false)
                            placeholder(R.drawable.ic_placeholder)
                            error(R.drawable.ic_placeholder)
                            listener(
                                onStart = { request ->
                                    Log.d(TAG, "Started loading TV image: ${request.data}")
                                },
                                onSuccess = { _, result ->
                                    Log.d(TAG, "TV Image loaded successfully from: ${result.dataSource}")
                                    supportStartPostponedEnterTransition()
                                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                    if (bitmap != null) {
                                        applyDynamicColors(bitmap)
                                    }
                                },
                                onError = { request, throwable ->
                                    Log.e(TAG, "TV Image load failed for ${request.data}: ${throwable.throwable?.message}", throwable.throwable)
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

    private fun applyDynamicColors(bitmap: android.graphics.Bitmap) {
        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
            palette?.let {
                val dominantColor = it.getDominantColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary))
                val vibrantColor = it.getVibrantColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_primary))
                val mutedColor = it.getMutedColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary))

                // Apply to CollapsingToolbar
                binding.toolbarLayout.setContentScrimColor(dominantColor)
                binding.toolbarLayout.setStatusBarScrimColor(dominantColor)
                
                // Apply to status bar
                window.statusBarColor = dominantColor

                // Apply to Action Buttons
                binding.btnAddToCollection.backgroundTintList = android.content.res.ColorStateList.valueOf(vibrantColor)
                
                // Category buttons (Optional: tint them too if needed, or keep themed)
                // binding.btnWatched.backgroundTintList = android.content.res.ColorStateList.valueOf(mutedColor) 
            }
        }
    }
}