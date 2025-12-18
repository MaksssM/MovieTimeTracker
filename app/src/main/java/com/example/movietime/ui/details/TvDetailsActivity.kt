package com.example.movietime.ui.details

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TvDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvDetailsBinding
    private val viewModel: TvDetailsViewModel by viewModels()

    @Inject
    lateinit var episodeService: TvShowEpisodeService

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
        binding = ActivityTvDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvId = intent.getIntExtra("ITEM_ID", -1)
        Log.d(TAG, "Opening TV show details: id=$tvId")

        if (tvId != -1) {
            Log.d(TAG, "Calling viewModel.loadTvShow($tvId)")
            viewModel.loadTvShow(tvId)
        } else {
            Log.e(TAG, "Invalid TV show ID: $tvId")
        }

        observeViewModel()
        
        // Додаємо плавну анімацію появи FAB
        binding.fabAdd.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .setStartDelay(200)
                .start()
        }

        // Обробник додавання в переглянуті
        binding.fabAdd.setOnClickListener {
            Log.d(TAG, "FAB clicked for TV show")
            val current = viewModel.tvShow.value
            Log.d(TAG, "Current tvShow value: ${if (current == null) "NULL" else current.name}, episodes: ${current?.numberOfEpisodes}")
            
            if (current == null) {
                Log.w(TAG, "Cannot add TV show: data is null")
                // Дані ще завантажуються, показуємо повідомлення
                Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.isItemWatched(current.id, "tv") { _ ->
                runOnUiThread {
                    // Show dialog regardless of existence to allow Editing
                    // If exists, the dialog will be pre-filled with observed data via ViewModel
                    showAddDialog(current)
                }
            }
        }
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
                    lastUpdated = System.currentTimeMillis()
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
                Log.d(TAG, "Displaying TV show: ${tvShow.name}, episodes: ${tvShow.numberOfEpisodes}, seasons: ${tvShow.numberOfSeasons}")
                binding.toolbarLayout.title = tvShow.name ?: getString(R.string.unknown_media)
                binding.tvOverview.text = tvShow.overview ?: getString(R.string.no_description_available)

                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.load(poster) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }
                
                // Анімація для контенту
                binding.tvOverview.apply {
                    alpha = 0f
                    translationY = 30f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .setStartDelay(100)
                        .start()
                }

                // Show season and episode counts (hide if null or 0)
                val seasonCount = tvShow.numberOfSeasons ?: 0
                if (seasonCount > 0) {
                    binding.tvSeasonCount.text = seasonCount.toString()
                    binding.tvSeasonCount.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvSeasonCount.visibility = android.view.View.GONE
                }

                val totalEpisodes = tvShow.numberOfEpisodes ?: 0
                if (totalEpisodes > 0) {
                    binding.tvTotalEpisodes.text = totalEpisodes.toString()
                    binding.tvTotalEpisodes.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvTotalEpisodes.visibility = android.view.View.GONE
                }

                // Initial calculation from API (fallback)
                updateTotalWatchTime(tvShow, viewModel.watchedItem.value)
            }
        }

        // Observe watched status to update runtime from DB
        viewModel.watchedItem.observe(this) { watchedItem ->
            val tvShow = viewModel.tvShow.value
            if (tvShow != null) {
                updateTotalWatchTime(tvShow, watchedItem)
                
                if (watchedItem != null) {
                    binding.fabAdd.text = getString(R.string.edit_watched_data)
                } else {
                    binding.fabAdd.text = getString(R.string.add_to_watched)
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
            binding.tvTotalWatchTime.visibility = android.view.View.VISIBLE
            
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
                    .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                    .start()
            }
            
            return
        }

        // Спочатку показуємо швидку оцінку (без "Завантаження...")
        val fallbackRuntimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
        val fallbackTime = Utils.formatMinutesToHoursAndMinutes(fallbackRuntimeInfo.totalMinutes)

        binding.tvTotalWatchTime.text = fallbackTime
        binding.tvTotalWatchTime.visibility = android.view.View.VISIBLE
        
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
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .start()
        }

        // Показуємо лише час без додаткових статусів чи уточнень
        // (залишаємо базове форматування з updateTotalWatchTime)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

