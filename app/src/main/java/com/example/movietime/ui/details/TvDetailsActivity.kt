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

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@TvDetailsActivity,
                                "Отримано точні дані: $episodes епізодів по $runtime хв",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@TvDetailsActivity,
                                "Використано розрахункові дані: $episodes епізодів по $runtime хв",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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

                // Розраховуємо загальний час
                val totalMinutes = episodes * runtime
                val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShow)

                // Створюємо елемент для збереження
                val item = WatchedItem(
                    id = tvShow.id,
                    title = tvShow.name ?: "",
                    posterPath = tvShow.posterPath,
                    releaseDate = tvShow.firstAirDate,
                    runtime = totalMinutes,
                    mediaType = "tv",
                    overview = tvShow.overview,
                    voteAverage = tvShow.voteAverage.toDouble(),
                    episodeRuntime = runtime,
                    totalEpisodes = episodes,
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
        viewModel.tvShow.observe(this) { tvShow ->
            if (tvShow != null) {
                binding.toolbarLayout.title = tvShow.name ?: getString(R.string.unknown_media)
                binding.tvOverview.text = tvShow.overview ?: getString(R.string.no_description_available)

                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.load(poster) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                // Show season and episode counts
                val seasonCount = tvShow.numberOfSeasons ?: 0
                binding.tvSeasonCount.text = seasonCount.toString()

                val totalEpisodes = tvShow.numberOfEpisodes ?: 0
                binding.tvTotalEpisodes.text = totalEpisodes.toString()

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
            
            val statusInfo = when {
                watchedItem.isOngoing -> getString(R.string.status_ongoing)
                watchedItem.status == "Ended" -> getString(R.string.status_ended)
                watchedItem.status == "Canceled" -> getString(R.string.status_canceled)
                else -> ""
            }
            
            val fullTimeText = if (statusInfo.isNotEmpty()) {
                getString(R.string.time_with_status, formattedTime, statusInfo)
            } else {
                formattedTime
            }
            
            binding.tvTotalWatchTime.text = fullTimeText
            return
        }

        // Спочатку показуємо швидку оцінку (без "Завантаження...")
        val fallbackRuntimeInfo = Utils.autoComputeTvShowRuntime(tvShow)
        val fallbackTime = Utils.formatMinutesToHoursAndMinutes(fallbackRuntimeInfo.totalMinutes)

        val statusInfo = when {
            fallbackRuntimeInfo.isOngoing -> " • Серіал виходить"
            tvShow.status == "Ended" -> " • Завершено"
            tvShow.status == "Canceled" -> " • Скасовано"
            else -> ""
        }

        binding.tvTotalWatchTime.text = "$fallbackTime (розрахунок)$statusInfo"

        // Намагаємось покращити дані в фоновому режимі (необов'язково)
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Trying to improve display data...")

                // Простий запит з коротким таймаутом
                val apiResult = withTimeoutOrNull(5000) {
                    episodeService.getQuickRuntimeEstimate(tvShow.id)
                }

                when (apiResult) {
                    is TvShowEpisodeService.QuickEstimateResult.Success -> {
                        val totalSeasons = tvShow.numberOfSeasons ?: 1
                        val estimatedTotal = apiResult.firstSeasonTotalMinutes * totalSeasons
                        val timeStr = Utils.formatMinutesToHoursAndMinutes(estimatedTotal)

                        runOnUiThread {
                            val statusInfo = when {
                                fallbackRuntimeInfo.isOngoing -> getString(R.string.status_ongoing)
                                tvShow.status == "Ended" -> getString(R.string.status_ended)
                                tvShow.status == "Canceled" -> getString(R.string.status_canceled)
                                else -> ""
                            }

                            val displayText = if (statusInfo.isNotEmpty()) {
                                getString(R.string.time_with_status, timeStr, statusInfo)
                            } else {
                                timeStr
                            }

                            val precision = if (apiResult.episodesWithRuntime > 0) {
                                getString(R.string.precision_with_api, apiResult.episodesWithRuntime)
                            } else {
                                getString(R.string.precision_calculated)
                            }

                            binding.tvTotalWatchTime.text = getString(R.string.time_with_precision, displayText, precision)
                        }
                    }
                    null -> {
                        Log.d(TAG, "API timeout, keeping calculation")
                    }
                    else -> {
                        Log.d(TAG, "API data not available, keeping calculation")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "API improvement failed: ${e.message}")
                // Користувач не побачить помилки - просто залишимо розрахункові дані
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

