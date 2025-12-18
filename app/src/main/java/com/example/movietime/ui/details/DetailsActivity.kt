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
import com.example.movietime.R
import android.util.Log
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
        setupCategoryButtons()
    }

    private fun setupCategoryButtons() {
        // Кнопка "Заплановані"
        binding.btnPlanned.setOnClickListener {
            addToCategory("planned")
        }

        // Кнопка "Переглянуті"
        binding.btnWatched.setOnClickListener {
            addToCategory("watched")
        }

        // Кнопка "Дивлюсь"
        binding.btnWatching.setOnClickListener {
            addToCategory("watching")
        }
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
            "planned" -> addToPlanned(id, title, posterPath, releaseDate, runtimeFromApi, mType)
            "watching" -> addToWatching(id, title, posterPath, releaseDate, runtimeFromApi, mType)
        }
    }

    private fun addToWatched(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String, current: Any) {
        viewModel.isItemWatched(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_watched), Toast.LENGTH_SHORT).show()
                }
            } else {
                val overview = when (current) {
                    is com.example.movietime.data.model.MovieResult -> current.overview
                    is com.example.movietime.data.model.TvShowResult -> current.overview
                    else -> null
                }
                val voteAverage = when (current) {
                    is com.example.movietime.data.model.MovieResult -> current.voteAverage?.toDouble()
                    is com.example.movietime.data.model.TvShowResult -> current.voteAverage?.toDouble()
                    else -> null
                }

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
                    userRating = null  // Без оцінки
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

    private fun addToPlanned(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String) {
        viewModel.isItemPlanned(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_planned), Toast.LENGTH_SHORT).show()
                }
            } else {
                val planned = Utils.createWatchedItemFromMovie(
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

    private fun addToWatching(id: Int, title: String?, posterPath: String?, releaseDate: String?, runtimeFromApi: Int?, mType: String) {
        viewModel.isItemWatching(id, mType) { exists ->
            if (exists) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.already_watching), Toast.LENGTH_SHORT).show()
                }
            } else {
                val watching = Utils.createWatchedItemFromMovie(
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
                null -> {
                    Log.w(TAG, "Item is null - failed to load data")
                    binding.toolbarLayout.title = getString(R.string.error_loading_data)
                    binding.tvOverview.text = getString(R.string.no_description_available)
                    binding.tvRuntime.text = "N/A"
                }
                else -> {
                    // nothing to show
                    Log.w(TAG, "Unknown item type: ${item?.javaClass?.simpleName}")
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

