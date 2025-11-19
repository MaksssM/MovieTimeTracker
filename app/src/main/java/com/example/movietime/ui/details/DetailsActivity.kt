package com.example.movietime.ui.details

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.movietime.databinding.ActivityDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.util.Utils
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.R
import java.util.Locale

@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private val viewModel: DetailsViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "en") ?: "en"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Обробник додавання в переглянуті
        binding.fabAdd.setOnClickListener {
            val current = viewModel.item.value
            if (current == null) {
                Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
                    return@setOnClickListener
                }
            }

            // Перевіримо, чи вже є в БД — передаём mediaType в ViewModel
            android.util.Log.d("DetailsActivity", "FAB clicked: id=$id, title=$title, mediaType=$mType")
            viewModel.isItemWatched(id, mType) { exists ->
                android.util.Log.d("DetailsActivity", "isItemWatched callback: exists=$exists")
                if (exists) {
                    runOnUiThread {
                        binding.fabAdd.text = getString(R.string.already_watched)
                        Toast.makeText(this, getString(R.string.already_watched), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.d("DetailsActivity", "Item not in DB, showing add dialog")
                    // If TV, ask how many episodes were watched to compute total runtime
                    if (mType == "tv") {
                        runOnUiThread {
                            val input = EditText(this)
                            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                            input.setText("1")

                            // Предпросмотр часа
                            val previewText = com.google.android.material.textview.MaterialTextView(this)
                            previewText.text = "Час: ${(runtimeFromApi ?: 0)}хв"
                            previewText.textSize = 14f
                            previewText.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                            previewText.setTextColor(getColor(R.color.secondary_text))

                            input.addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    val episodes = s.toString().toIntOrNull() ?: 1
                                    val totalMinutes = (runtimeFromApi ?: 0) * episodes
                                    val hours = totalMinutes / 60
                                    val minutes = totalMinutes % 60
                                    previewText.text = "Час: ${hours}г ${minutes}хв (${episodes} епізодів)"
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            })

                            val container = android.widget.LinearLayout(this)
                            container.orientation = android.widget.LinearLayout.VERTICAL
                            container.addView(input, android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(16, 16, 16, 8) })
                            container.addView(previewText, android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(16, 8, 16, 16) })

                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.enter_episodes_watched))
                                .setMessage(getString(R.string.enter_episodes_watched_hint))
                                .setView(container)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    val episodes = input.text.toString().toIntOrNull() ?: 1
                                    val totalRuntime = (runtimeFromApi ?: 0) * episodes

                                    android.util.Log.d("DetailsActivity", "Creating TV watched item: episodes=$episodes, totalRuntime=$totalRuntime")

                                    val overview = when (current) {
                                        is com.example.movietime.data.model.TvShowResult -> current.overview
                                        else -> null
                                    }
                                    val voteAverage = when (current) {
                                        is com.example.movietime.data.model.TvShowResult -> current.voteAverage?.toDouble()
                                        else -> null
                                    }

                                    val watched = Utils.createWatchedItemFromMovie(
                                        id = id,
                                        title = title,
                                        name = null,
                                        posterPath = posterPath,
                                        releaseDate = releaseDate,
                                        runtime = totalRuntime,
                                        mediaType = mType,
                                        overview = overview,
                                        voteAverage = voteAverage
                                    )

                                    android.util.Log.d("DetailsActivity", "Created watched item: $watched")

                                    viewModel.addWatchedItem(watched) { success ->
                                        android.util.Log.d("DetailsActivity", "addWatchedItem callback for TV: success=$success")
                                        runOnUiThread {
                                            if (success) {
                                                android.util.Log.d("DetailsActivity", "Successfully added TV show to DB")
                                                try { binding.fabAdd.setIconResource(0) } catch (_: Throwable) {}
                                                binding.fabAdd.text = getString(R.string.added)
                                                val hours = totalRuntime / 60
                                                val minutes = totalRuntime % 60
                                                Toast.makeText(this, "Додано: $episodes епізодів (${hours}г ${minutes}хв)", Toast.LENGTH_LONG).show()
                                            } else {
                                                android.util.Log.e("DetailsActivity", "Failed to add TV show to DB")
                                                Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                    } else {
                        // Movie: add as-is, but ask for rating first
                        android.util.Log.d("DetailsActivity", "Showing rating dialog for movie")
                        val overview = when (current) {
                            is com.example.movietime.data.model.MovieResult -> current.overview
                            else -> null
                        }
                        val voteAverage = when (current) {
                            is com.example.movietime.data.model.MovieResult -> current.voteAverage?.toDouble()
                            else -> null
                        }

                        showRatingDialog { userRating ->
                            android.util.Log.d("DetailsActivity", "Rating selected: $userRating")
                            val watched = Utils.createWatchedItemFromMovie(
                                id = id,
                                title = title,
                                name = null,
                                posterPath = posterPath,
                                releaseDate = releaseDate,
                                runtime = runtimeFromApi,
                                mediaType = mType,
                                overview = overview,
                                voteAverage = voteAverage,
                                userRating = userRating
                            )

                            android.util.Log.d("DetailsActivity", "Created movie watched item: $watched")

                            viewModel.addWatchedItem(watched) { success ->
                                android.util.Log.d("DetailsActivity", "addWatchedItem callback: success=$success")
                                runOnUiThread {
                                    if (success) {
                                        android.util.Log.d("DetailsActivity", "Successfully added movie to DB")
                                        try { binding.fabAdd.setIconResource(0) } catch (_: Throwable) {}
                                        binding.fabAdd.text = getString(R.string.added)
                                        Toast.makeText(this, getString(R.string.added_to_watched_toast), Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.util.Log.e("DetailsActivity", "Failed to add movie to DB")
                                        Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            when (item) {
                is com.example.movietime.data.model.MovieResult -> {
                    binding.toolbarLayout.title = item.title ?: getString(R.string.unknown_media)
                    binding.tvOverview.text = item.overview ?: getString(R.string.no_description_available)

                    // Display runtime
                    val runtime = item.runtime ?: 0
                    binding.tvRuntime.text = if (runtime > 0) {
                        val hours = runtime / 60
                        val minutes = runtime % 60
                        if (hours > 0 && minutes > 0) {
                            "$hours год $minutes хв"
                        } else if (hours > 0) {
                            "$hours год"
                        } else {
                            "$minutes хв"
                        }
                    } else {
                        "N/A"
                    }

                    val poster = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(poster) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                }
                is com.example.movietime.data.model.TvShowResult -> {
                    binding.toolbarLayout.title = item.name ?: getString(R.string.unknown_media)
                    binding.tvOverview.text = item.overview ?: getString(R.string.no_description_available)

                    // Display episode runtime
                    val episodeRuntime = item.episodeRunTime?.firstOrNull() ?: 0
                    binding.tvRuntime.text = if (episodeRuntime > 0) {
                        "$episodeRuntime хв / епізод"
                    } else {
                        "N/A"
                    }

                    val poster = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(poster) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                }
                else -> {
                    // nothing to show
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}