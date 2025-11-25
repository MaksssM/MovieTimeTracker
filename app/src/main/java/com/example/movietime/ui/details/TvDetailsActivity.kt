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
import java.util.Locale

@AndroidEntryPoint
class TvDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvDetailsBinding
    private val viewModel: TvDetailsViewModel by viewModels()

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
            android.util.Log.d("TvDetailsActivity", "FAB clicked, current item: $current")
            if (current == null) {
                android.util.Log.w("TvDetailsActivity", "Cannot add to watched: data not loaded yet")
                Toast.makeText(this, getString(R.string.data_not_loaded) + "\nПеревірте з'єднання з інтернетом", Toast.LENGTH_LONG).show()
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
                            mediaType = "tv",
                            overview = current.overview,
                            voteAverage = current.voteAverage.toDouble()
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
                binding.tvOverview.text = tvShow.overview ?: getString(R.string.no_description_available)

                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.load(poster) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

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