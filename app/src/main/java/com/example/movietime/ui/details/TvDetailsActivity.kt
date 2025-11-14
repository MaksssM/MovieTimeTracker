package com.example.movietime.ui.details

import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.movietime.databinding.ActivityTvDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.util.Utils
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.R

@AndroidEntryPoint
class TvDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvDetailsBinding
    private val viewModel: TvDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvId = intent.getIntExtra("TV_ID", -1)

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

            val id = current.id
            val title = current.name ?: current.title
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
                    // Питаємо кількість переглянутих серій
                    runOnUiThread {
                        val allSeasons = current.seasons ?: emptyList()
                        // Фільтруємо сезони: виключаємо сезон 0 (спеціальні епізоди)
                        val seasons = allSeasons.filter { it.season_number > 0 }
                        if (seasons.isEmpty()) {
                            Toast.makeText(this, getString(R.string.no_seasons_found), Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        // Спінер для вибору сезону
                        val spinner = Spinner(this)
                        val seasonNames = seasons.map { season ->
                            season.name?.takeIf { it.isNotBlank() } ?: "Сезон ${season.season_number}"
                        }
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasonNames)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter

                        // NumberPicker для вибору кількості епізодів
                        val picker = NumberPicker(this)
                        picker.minValue = 1
                        picker.maxValue = seasons[0].episode_count
                        picker.value = 1
                        picker.wrapSelectorWheel = false
                        picker.setFormatter { it.toString() }
                        picker.contentDescription = getString(R.string.enter_episodes_watched_hint)

                        // Текст для показу інформації про сезон
                        val infoText = TextView(this)
                        infoText.text = "Сезон 1 має ${seasons[0].episode_count} епізодів"
                        infoText.textSize = 14f
                        infoText.gravity = android.view.Gravity.CENTER

                        // Обробник зміни сезону
                        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                if (position < seasons.size) {
                                    val selectedSeason = seasons[position]
                                    val episodeCount = selectedSeason.episode_count
                                    picker.maxValue = episodeCount
                                    picker.value = episodeCount // За замовчуванням вибираємо весь сезон

                                    // Оновлюємо інформацію про сезон
                                    val seasonNum = selectedSeason.season_number
                                    val episodesWatched = picker.value
                                    val runtime = episodeRuntime * episodesWatched

                                    infoText.text = "Сезон $seasonNum: $episodeCount епізодів\n" +
                                            "Переглянуто: $episodesWatched епізодів\n" +
                                            "Час: ${runtime / 60}г ${runtime % 60}хв"
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                        // Обробник зміни кількості епізодів
                        picker.setOnValueChangedListener { _, _, newVal ->
                            if (spinner.selectedItemPosition < seasons.size) {
                                val selectedSeason = seasons[spinner.selectedItemPosition]
                                val seasonNum = selectedSeason.season_number
                                val runtime = episodeRuntime * newVal

                                infoText.text = "Сезон $seasonNum: ${selectedSeason.episode_count} епізодів\n" +
                                        "Переглянуто: $newVal епізодів\n" +
                                        "Час: ${runtime / 60}г ${runtime % 60}хв"
                            }
                        }

                        val container = LinearLayout(this)
                        container.orientation = LinearLayout.VERTICAL
                        val margin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

                        val infoParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        infoParams.setMargins(margin, margin, margin, margin)
                        infoText.layoutParams = infoParams

                        val spinnerParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        spinnerParams.setMargins(margin, 0, margin, margin)
                        spinner.layoutParams = spinnerParams

                        val pickerParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        pickerParams.setMargins(margin, 0, margin, margin)
                        picker.layoutParams = pickerParams

                        container.addView(infoText)
                        container.addView(spinner)
                        container.addView(picker)

                        val dialog = AlertDialog.Builder(this)
                            .setTitle(getString(R.string.enter_episodes_watched))
                            .setMessage("Виберіть сезон та кількість епізодів, які ви переглянули")
                            .setView(container)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val selectedSeasonIdx = spinner.selectedItemPosition
                                val episodesWatched = picker.value

                                if (selectedSeasonIdx < seasons.size) {
                                    val selectedSeason = seasons[selectedSeasonIdx]

                                    // Розраховуємо всі епізоди до обраного сезону (не включно)
                                    var totalEpisodes = 0
                                    for (i in 0 until selectedSeasonIdx) {
                                        if (i < seasons.size) {
                                            totalEpisodes += seasons[i].episode_count
                                        }
                                    }

                                    // Додаємо переглянуті епізоди з обраного сезону
                                    totalEpisodes += episodesWatched

                                    // Розраховуємо загальний час
                                    val totalRuntime = episodeRuntime * totalEpisodes

                                    val seasonNum = selectedSeason.season_number
                                    val totalHours = totalRuntime / 60
                                    val totalMinutes = totalRuntime % 60

                                    val watched = Utils.createWatchedItemFromMovie(
                                        id = id,
                                        title = title,
                                        name = null,
                                        posterPath = posterPath,
                                        releaseDate = releaseDate,
                                        runtime = totalRuntime,
                                        mediaType = "tv"
                                    )
                                    viewModel.addWatchedItem(watched) { success ->
                                        runOnUiThread {
                                            if (success) {
                                                try {
                                                    binding.fabAdd.setIconResource(R.drawable.ic_check)
                                                } catch (_: Throwable) {}
                                                binding.fabAdd.text = getString(R.string.added)
                                                binding.fabAdd.setBackgroundColor(getColor(R.color.md_theme_light_primary))
                                                Toast.makeText(
                                                    this,
                                                    "Додано: $totalEpisodes епізодів ($totalHours год $totalMinutes хв)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(this, getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                        dialog.show()
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.md_theme_light_primary))
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.md_theme_light_secondary))
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.tvShow.observe(this) { tvShow ->
            if (tvShow != null) {
                binding.toolbarLayout.title = tvShow.name ?: tvShow.title ?: getString(R.string.unknown_media)
                binding.tvOverview.text = tvShow.overview
                val poster = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                binding.ivPoster.load(poster)

                // Показуємо кількість сезонів та епізодів
                val seasonCount = tvShow.seasons?.size ?: 0
                binding.tvSeasonCount.text = seasonCount.toString()

                val totalEpisodes = tvShow.seasons?.sumOf { it.episode_count } ?: 0
                binding.tvTotalEpisodes.text = totalEpisodes.toString()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}