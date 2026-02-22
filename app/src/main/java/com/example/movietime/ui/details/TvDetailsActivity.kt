package com.example.movietime.ui.details

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.movietime.databinding.ActivityTvDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.R
import com.example.movietime.util.Utils
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.service.TvShowEpisodeService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.example.movietime.utils.HapticFeedbackHelper


@AndroidEntryPoint
class TvDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvDetailsBinding
    private val viewModel: TvDetailsViewModel by viewModels()

    @Inject
    lateinit var episodeService: TvShowEpisodeService
    
    private var currentTvShow: TvShowResult? = null

    companion object {
        private const val TAG = "TvDetailsActivity"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
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
        
        // Enable shared element transitions
        window.requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS)
        
        // Activity transition
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.activity_open_enter, R.anim.smooth_fade_out)
        
        // Postpone enter transition until poster is loaded
        supportPostponeEnterTransition()
        
        binding = ActivityTvDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val tvId = intent.getIntExtra("ITEM_ID", -1)
        Log.d(TAG, "Opening TV show details: id=$tvId")

        if (tvId != -1) {
            Log.d(TAG, "Calling viewModel.loadTvShow($tvId)")
            viewModel.loadTvShow(tvId)
        } else {
            Log.e(TAG, "Invalid TV show ID: $tvId")
        }

        observeViewModel()
        setupClickListeners()
        animateEntranceElements()
        
        // Fallback: start postponed transition after timeout if poster hasn't loaded
        binding.ivPoster.postDelayed({ supportStartPostponedEnterTransition() }, 500)

        // Обробник додавання в переглянуті - відкриває вибір епізодів
        binding.fabAdd.setOnClickListener { view ->
            HapticFeedbackHelper.impactLow(view)
            animateButtonPress(view) {
                Log.d(TAG, "FAB clicked for TV show")
                val current = viewModel.tvShow.value
                Log.d(TAG, "Current tvShow value: ${if (current == null) "NULL" else current.name}, episodes: ${current?.numberOfEpisodes}")
            
                if (current == null) {
                    Log.w(TAG, "Cannot add TV show: data is null")
                    // Дані ще завантажуються, показуємо повідомлення
                    Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
                    return@animateButtonPress
                }

                // Відкриваємо BottomSheet для вибору епізодів
                showEpisodeProgressSheet(current)
            }
        }
    }
    
    private fun showEpisodeProgressSheet(tvShow: TvShowResult) {
        val bottomSheet = TvProgressBottomSheet.newInstance(tvShow.id) { watchedRuntime ->
            // Callback коли прогрес збережено
            runOnUiThread {
                if (watchedRuntime > 0) {
                    binding.fabAdd.text = getString(R.string.edit_progress)
                    binding.tvTotalWatchTime.text = Utils.formatMinutesToHoursAndMinutes(watchedRuntime)
                } else {
                    binding.fabAdd.text = getString(R.string.track_progress)
                }
            }
        }
        bottomSheet.show(supportFragmentManager, "TvProgressBottomSheet")
    }
    
    private fun setupClickListeners() {
        // Planned button
    binding.btnPlanned.setOnClickListener { view ->
        animateButtonPress(view) {
            val tvShow = currentTvShow ?: return@animateButtonPress
            addToPlanned(tvShow)
        }
    }
    
    // Watched button
    binding.btnWatched.setOnClickListener { view ->
        animateButtonPress(view) {
            binding.fabAdd.performClick()
        }
    }
    
    // Watching button
    binding.btnWatching.setOnClickListener { view ->
        animateButtonPress(view) {
            val tvShow = currentTvShow ?: return@animateButtonPress
            addToWatching(tvShow)
        }
    }
    
    // Rate button
    binding.btnRate.setOnClickListener { view ->
        animateButtonPress(view) {
            Toast.makeText(this, getString(R.string.rate_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }
    
    // Share button
    binding.btnShare.setOnClickListener { view ->
        animateButtonPress(view) {
            val tvShow = currentTvShow ?: return@animateButtonPress
            shareTvShow(tvShow)
        }
    }

    // Add to Collection button
    binding.btnAddToCollection.setOnClickListener { view ->
        animateButtonPress(view) {
            val tvShow = currentTvShow ?: return@animateButtonPress
            val bottomSheet = com.example.movietime.ui.collections.AddToCollectionBottomSheet.newInstance(
                itemId = tvShow.id,
                mediaType = "tv",
                title = tvShow.name,
                posterPath = tvShow.posterPath
            )
            bottomSheet.show(supportFragmentManager, "AddToCollectionBottomSheet")
        }
    }
    }
    
    private fun addToPlanned(tvShow: TvShowResult) {
        viewModel.isItemPlanned(tvShow.id) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_planned), Toast.LENGTH_SHORT).show()
                }
            } else {
                val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
                val plannedItem = WatchedItem(
                    id = tvShow.id,
                    title = tvShow.name ?: "",
                    posterPath = tvShow.posterPath,
                    releaseDate = tvShow.firstAirDate,
                    runtime = runtimeInfo.totalMinutes,
                    mediaType = "tv",
                    overview = tvShow.overview,
                    voteAverage = tvShow.voteAverage.toDouble(),
                    episodeRuntime = runtimeInfo.episodeRuntime,
                    totalEpisodes = runtimeInfo.episodes,
                    isOngoing = runtimeInfo.isOngoing,
                    genreIds = tvShow.genreIds?.joinToString(",") ?: tvShow.genres?.map { it.id }?.joinToString(",")
                )
                
                viewModel.addToPlanned(plannedItem) { success ->
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
    
    private fun addToWatching(tvShow: TvShowResult) {
        viewModel.isItemWatching(tvShow.id) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_watching), Toast.LENGTH_SHORT).show()
                }
            } else {
                val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
                val watchingItem = WatchedItem(
                    id = tvShow.id,
                    title = tvShow.name ?: "",
                    posterPath = tvShow.posterPath,
                    releaseDate = tvShow.firstAirDate,
                    runtime = runtimeInfo.totalMinutes,
                    mediaType = "tv",
                    overview = tvShow.overview,
                    voteAverage = tvShow.voteAverage.toDouble(),
                    episodeRuntime = runtimeInfo.episodeRuntime,
                    totalEpisodes = runtimeInfo.episodes,
                    isOngoing = runtimeInfo.isOngoing,
                    genreIds = tvShow.genreIds?.joinToString(",") ?: tvShow.genres?.map { it.id }?.joinToString(",")
                )
                
                viewModel.addToWatching(watchingItem) { success ->
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
    
    private fun disableButton(view: View) {
        view.alpha = 0.5f
        view.isClickable = false
        view.isFocusable = false
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
    
    private fun shareTvShow(tvShow: TvShowResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tvShow.name ?: "")
            putExtra(Intent.EXTRA_TEXT, buildString {
                append("📺 ${tvShow.name}\n")
                append("⭐ ${String.format(Locale.US, "%.1f", tvShow.voteAverage)}/10\n")
                tvShow.firstAirDate?.take(4)?.let { append("📅 $it\n") }
                append("\n${tvShow.overview ?: ""}")
            })
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }



    private fun showAddDialog(tvShow: TvShowResult) {
        // Показуємо прогрес
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            .setCancelable(false)
            .create()
        
        progressDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog.show()

        // Автоматично отримуємо дані
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Автоматичне отримання даних про серіал...")

                // Використовуємо точний розрахунок
                val exactResult = episodeService.getExactTvShowRuntime(
                    tvShow.id,
                    tvShow.numberOfSeasons ?: 1
                )

                val episodes: Int
                val runtime: Int

                when (exactResult) {
                    is TvShowEpisodeService.ExactRuntimeResult.Success -> {
                        Log.d(TAG, "Отримано точні дані з API")
                        val info = exactResult.runtimeInfo
                        episodes = info.totalEpisodes
                        runtime = info.averageEpisodeRuntime

                        // Дані отримано успішно
                    }
                    is TvShowEpisodeService.ExactRuntimeResult.Error -> {
                        Log.d(TAG, "Точний розрахунок не вдався, використовуємо оцінку")
                        // Використовуємо швидку оцінку
                        val quickResult = episodeService.getQuickRuntimeEstimate(tvShow.id)

                        when (quickResult) {
                            is TvShowEpisodeService.QuickEstimateResult.Success -> {
                                episodes = tvShow.numberOfEpisodes ?: 0
                                runtime = quickResult.averageEpisodeRuntime
                            }
                            else -> {
                                // Якщо все не вдалося, використовуємо дефолтні дані
                                val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
                                episodes = runtimeInfo.episodes
                                runtime = runtimeInfo.episodeRuntime
                            }
                        }

                        // Використано розрахункові дані
                    }
                }

                // Перевіряємо валідність даних
                if (episodes <= 0 || runtime <= 0) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TvDetailsActivity,
                            "Не вдалося отримати дані про серіал. Спробуйте пізніше.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                Log.d(TAG, "Додаємо серіал: episodes=$episodes, runtime=$runtime")

                // Розраховуємо загальний час за допомогою спеціалізованого методу
                val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShow)

                // Створюємо елемент для збереження
                val item = WatchedItem(
                    id = tvShow.id,
                    title = tvShow.name ?: "",
                    posterPath = tvShow.posterPath,
                    releaseDate = tvShow.firstAirDate,
                    runtime = runtimeInfo.totalMinutes,
                    mediaType = "tv",
                    overview = tvShow.overview,
                    voteAverage = tvShow.voteAverage.toDouble(),
                    episodeRuntime = runtimeInfo.episodeRuntime,
                    totalEpisodes = runtimeInfo.episodes,
                    isOngoing = runtimeInfo.isOngoing,
                    status = tvShow.status,
                    lastUpdated = System.currentTimeMillis(),
                    genreIds = tvShow.genreIds?.joinToString(",") ?: tvShow.genres?.map { it.id }?.joinToString(",")
                )

                Log.d(TAG, "Створено WatchedItem: ${item.title}, totalMinutes=${item.runtime}, ongoing=${item.isOngoing}")

                viewModel.addWatchedItem(item) { success ->
                    runOnUiThread {
                        progressDialog.dismiss()

                        if (success) {
                            Log.d(TAG, "Успішно додано серіал до переглянутих")
                            Toast.makeText(
                                this@TvDetailsActivity,
                                "Додано: ${item.title} (${Utils.formatMinutesToHoursAndMinutes(item.runtime)})",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.fabAdd.text = getString(R.string.added)
                            binding.tvTotalWatchTime.text = Utils.formatMinutesToHoursAndMinutes(item.runtime)
                        } else {
                            Log.e(TAG, "Не вдалося додати серіал")
                            Toast.makeText(
                                this@TvDetailsActivity,
                                getString(R.string.add_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Помилка при автоматичному додаванні: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@TvDetailsActivity,
                        "Помилка: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Setting up tvShow observer")
        viewModel.tvShow.observe(this) { tvShow ->
            Log.d(TAG, "tvShow observer triggered: ${if (tvShow == null) "NULL" else tvShow.name}")
            if (tvShow != null) {
                currentTvShow = tvShow
                Log.d(TAG, "Displaying TV show: ${tvShow.name}, episodes: ${tvShow.numberOfEpisodes}, seasons: ${tvShow.numberOfSeasons}")
                
                // Title in floating card
                binding.tvTitle.text = tvShow.name ?: getString(R.string.unknown_media)
                binding.toolbarLayout.title = ""
                
                // Overview
                binding.tvOverview.text = tvShow.overview ?: getString(R.string.no_description_available)

                // Poster
                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.transitionName = "poster_transition"
                binding.ivPoster.load(poster) {
                    crossfade(true)
                    allowHardware(false)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                    listener(
                        onSuccess = { _, result ->
                            supportStartPostponedEnterTransition()
                            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                applyDynamicColors(bitmap)
                            }
                        },
                        onError = { _, _ ->
                            supportStartPostponedEnterTransition()
                        }
                    )
                }
                
                // Rating in floating card
                binding.tvRating.text = String.format(Locale.US, "%.1f", tvShow.voteAverage)
                
                // Rating percentage in stat card
                val ratingPercent = (tvShow.voteAverage * 10).toInt()
                binding.progressRating.progress = ratingPercent
                binding.tvRatingPercent.text = "$ratingPercent%"
                
                // Year range
                val startYear = tvShow.firstAirDate?.take(4) ?: ""
                val endYear = if (tvShow.status == "Ended" || tvShow.status == "Canceled") {
                    tvShow.lastAirDate?.take(4) ?: ""
                } else {
                    getString(R.string.present)
                }
                binding.tvYear.text = if (startYear.isNotEmpty()) "$startYear - $endYear" else ""
                
                // Season count
                val seasonCount = tvShow.numberOfSeasons ?: 0
                if (seasonCount > 0) {
                    binding.tvSeasonCount.text = seasonCount.toString()
                    binding.tvSeasonCount.visibility = View.VISIBLE
                } else {
                    binding.tvSeasonCount.visibility = View.GONE
                }

                // Episode count
                val totalEpisodes = tvShow.numberOfEpisodes ?: 0
                if (totalEpisodes > 0) {
                    binding.tvTotalEpisodes.text = totalEpisodes.toString()
                    binding.tvTotalEpisodes.visibility = View.VISIBLE
                } else {
                    binding.tvTotalEpisodes.visibility = View.GONE
                }
                
                // Additional info
                binding.tvFirstAirDate.text = formatDate(tvShow.firstAirDate)
                binding.tvLastAirDate.text = formatDate(tvShow.lastAirDate)
                
                // Episode runtime
                val episodeRuntime = tvShow.episodeRunTime?.firstOrNull()
                binding.tvEpisodeRuntime.text = if (episodeRuntime != null) "~$episodeRuntime хв" else "—"
                
                // Status with color
                binding.tvStatus.text = translateStatus(tvShow.status)
                binding.tvStatus.setTextColor(getStatusColor(tvShow.status))
                
                // Vote count
                binding.tvVoteCount.text = NumberFormat.getNumberInstance(Locale.US).format(tvShow.voteCount)
                
                // Genres chips
                setupGenreChips(tvShow.genres)

                // Animation for content
                animateContentAppearance()

                // Initial calculation from API (fallback)
                updateTotalWatchTime(tvShow, viewModel.watchedItem.value)
            }
        }

        // Observe watched status to update runtime from DB
        viewModel.watchedItem.observe(this) { watchedItem ->
            val tvShow = viewModel.tvShow.value
            if (tvShow != null) {
                updateTotalWatchTime(tvShow, watchedItem)
                
                // Update status badges
                binding.cardWatchedBadge.visibility = View.GONE
                binding.cardInProgressBadge.visibility = View.GONE
                
                if (watchedItem != null) {
                    binding.fabAdd.text = getString(R.string.edit_progress)
                    
                    // Check if fully watched
                    val totalEpisodes = watchedItem.totalEpisodes ?: 0
                    val episodeRuntime = watchedItem.episodeRuntime ?: 1
                    val watchedRuntime = watchedItem.runtime ?: 0
                    val watchedEpisodes = if (episodeRuntime > 0) watchedRuntime / episodeRuntime else 0
                    val isOngoing = watchedItem.isOngoing
                    
                    if (totalEpisodes > 0 && watchedEpisodes >= totalEpisodes && !isOngoing) {
                        // Fully watched - show with user rating if available and animated
                        animateBadgeAppear(binding.cardWatchedBadge)
                        val userRating = watchedItem.userRating
                        if (userRating != null && userRating > 0) {
                            binding.tvWatchedBadge.text = getString(R.string.fully_watched) + " ⭐ ${userRating.toInt()}"
                        } else {
                            binding.tvWatchedBadge.text = getString(R.string.fully_watched)
                        }
                        
                        // Disable buttons since fully watched
                        disableButton(binding.btnWatched)
                        disableButton(binding.btnPlanned)
                        disableButton(binding.btnWatching)
                    } else if (watchedEpisodes > 0) {
                        // In progress with detailed info and animation
                        animateBadgeAppear(binding.cardInProgressBadge)
                        val progressPercent = if (totalEpisodes > 0) (watchedEpisodes * 100) / totalEpisodes else 0
                        binding.tvInProgressBadge.text = "$watchedEpisodes/$totalEpisodes ($progressPercent%)"
                        
                        // Disable watching button since already tracking
                        disableButton(binding.btnWatching)
                    }
                } else {
                    binding.fabAdd.text = getString(R.string.track_progress)
                    
                    // Check if in planned or watching
                    viewModel.isItemPlanned(tvShow.id) { isPlanned ->
                        if (isPlanned) {
                            runOnUiThread {
                                disableButton(binding.btnPlanned)
                            }
                        }
                    }
                    viewModel.isItemWatching(tvShow.id) { isWatching ->
                        if (isWatching) {
                            runOnUiThread {
                                animateBadgeAppear(binding.cardInProgressBadge)
                                binding.tvInProgressBadge.text = getString(R.string.watching)
                                disableButton(binding.btnWatching)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateTotalWatchTime(tvShow: com.example.movietime.data.model.TvShowResult, watchedItem: WatchedItem?) {
        if (watchedItem != null && watchedItem.runtime != null && watchedItem.runtime > 0) {
            // Priority: Use the time from the database (user input)
            Log.d(TAG, "Using runtime from DB: ${watchedItem.runtime}")
            val formattedTime = Utils.formatMinutesToHoursAndMinutes(watchedItem.runtime)
            
            binding.tvTotalWatchTime.text = formattedTime
            binding.tvTotalWatchTime.visibility = View.VISIBLE
            
            // Анімація оновлення тексту
            binding.tvTotalWatchTime.apply {
                alpha = 0f
                scaleX = 0.92f
                scaleY = 0.92f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(420)
                    .setInterpolator(OvershootInterpolator(0.8f))
                    .start()
            }
            
            return
        }

        // Спочатку показуємо швидку оцінку (без "Завантаження...")
        val fallbackRuntimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
        val fallbackTime = Utils.formatMinutesToHoursAndMinutes(fallbackRuntimeInfo.totalMinutes)

        binding.tvTotalWatchTime.text = fallbackTime
        binding.tvTotalWatchTime.visibility = View.VISIBLE
        
        // Плавна анімація появи
        binding.tvTotalWatchTime.apply {
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }

        // Показуємо лише час без додаткових статусів чи уточнень
        // (залишаємо базове форматування з updateTotalWatchTime)
    }
    
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "—"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun translateStatus(status: String?): String {
        return when (status) {
            "Returning Series" -> getString(R.string.status_returning)
            "Ended" -> getString(R.string.status_ended)
            "Canceled" -> getString(R.string.status_canceled)
            "In Production" -> getString(R.string.status_in_production)
            "Planned" -> getString(R.string.status_planned)
            "Pilot" -> getString(R.string.status_pilot)
            else -> status ?: "—"
        }
    }
    
    private fun getStatusColor(status: String?): Int {
        return when (status) {
            "Returning Series" -> 0xFF22C55E.toInt() // Green
            "Ended" -> 0xFF60A5FA.toInt() // Blue
            "Canceled" -> 0xFFEF4444.toInt() // Red
            "In Production" -> 0xFFFBBF24.toInt() // Yellow
            else -> 0xFFFFFFFF.toInt() // White
        }
    }
    
    private fun setupGenreChips(genres: List<com.example.movietime.data.model.Genre>?) {
        binding.chipGroupGenres.removeAllViews()
        genres?.take(4)?.forEach { genre ->
            val chip = Chip(this).apply {
                text = genre.name
                isClickable = false
                setChipBackgroundColorResource(android.R.color.transparent)
                setChipStrokeColorResource(R.color.chip_stroke_color)
                chipStrokeWidth = 1.5f * resources.displayMetrics.density
                setTextColor(resources.getColor(R.color.chip_text_color, theme))
                textSize = 12f
                chipMinHeight = 32f * resources.displayMetrics.density
            }
            binding.chipGroupGenres.addView(chip)
        }
    }
    
    private fun animateContentAppearance() {
        // Animate floating card
        binding.cardFloatingInfo.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .setStartDelay(100)
                .start()
        }
        
        // Animate overview section
        binding.tvOverview.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .setStartDelay(200)
                .start()
        }
    }
    
    private fun animateEntranceElements() {
        // Animate FAB entrance with bounce effect
        binding.fabAdd.apply {
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
        }
        
        lifecycleScope.launch {
            delay(300)
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.fabAdd, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(binding.fabAdd, "scaleX", 0f, 1.1f, 1f),
                    ObjectAnimator.ofFloat(binding.fabAdd, "scaleY", 0f, 1.1f, 1f)
                )
                duration = 450
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }
    }
    
    private fun animateButtonPress(view: View, action: () -> Unit) {
        com.example.movietime.utils.HapticFeedbackHelper.impactLow(view)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1.05f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1.05f, 1f)
            )
            duration = 250
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
        view.postDelayed({ action() }, 120)
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
                
                // Apply to CollapsingToolbar
                binding.toolbarLayout.setContentScrimColor(dominantColor)
                binding.toolbarLayout.setStatusBarScrimColor(dominantColor)
                
                // Apply to status bar
                window.statusBarColor = dominantColor

                // Apply to buttons
                binding.fabAdd.backgroundTintList = android.content.res.ColorStateList.valueOf(vibrantColor)
                binding.btnAddToCollection.backgroundTintList = android.content.res.ColorStateList.valueOf(vibrantColor)
            }
        }
    }
}