package com.example.movietime.ui.search

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.databinding.ActivitySearchBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.widget.addTextChangedListener
import com.example.movietime.R
import java.util.*

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set toolbar as support action bar and enable navigation
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeViewModel()

        // Завантажити збережені налаштування
        viewModel.loadFilterPreferences(this)

        // End icon (custom) click: clear query
        binding.tilSearch.setEndIconOnClickListener {
            binding.etSearchQuery.text?.clear()
            binding.etSearchQuery.clearFocus()
            viewModel.searchMulti("")
            binding.etSearchQuery.requestFocus()
            // Hide keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchQuery.windowToken, 0)
        }

        // If launched with a test query (adb), trigger search immediately for reliable testing
        intent?.getStringExtra("TEST_QUERY")?.let { testQ ->
            if (testQ.isNotBlank()) {
                android.util.Log.d("SearchActivity", "TEST_QUERY received: $testQ")
                // populate UI for visibility
                binding.etSearchQuery.setText(testQ)
                // trigger search without debounce
                viewModel.searchMulti(testQ)
            }
        }

        binding.sliderMinRating.addOnChangeListener { _: Slider, value: Float, _: Boolean ->
            viewModel.setMinRating(value.toDouble())
        }
    }

    override fun onPause() {
        super.onPause()
        // Зберегти налаштування при виході
        viewModel.saveFilterPreferences(this)
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter()

        searchAdapter.onItemClick = { item ->
            when (item) {
                is MovieResult -> {
                    val intent = Intent(this, DetailsActivity::class.java)
                    intent.putExtra("ITEM_ID", item.id)
                    intent.putExtra("MEDIA_TYPE", "movie")
                    startActivity(intent)
                }
                is TvShowResult -> {
                    val intent = Intent(this, TvDetailsActivity::class.java)
                    intent.putExtra("TV_ID", item.id)
                    startActivity(intent)
                }
            }
        }

        searchAdapter.onItemLongClick = { item ->
            when (item) {
                is MovieResult -> Toast.makeText(this, "Довгий тап: ${item.title} (видалено зі списку)", Toast.LENGTH_SHORT).show()
                is TvShowResult -> Toast.makeText(this, "Довгий тап: ${item.name} (видалено зі списку)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
            // Improve performance for fixed size items
            setHasFixedSize(true)
        }
    }

    private fun setupFilters() {
        // Filter by type - встановлюємо перший фільтр як активний
        binding.btnFilterAll.isSelected = true

        binding.btnFilterAll.setOnClickListener {
            updateFilterButtons(0)
            viewModel.setFilterType(SearchViewModel.FilterType.ALL)
        }

        binding.btnFilterMovies.setOnClickListener {
            updateFilterButtons(1)
            viewModel.setFilterType(SearchViewModel.FilterType.MOVIES)
        }

        binding.btnFilterTv.setOnClickListener {
            updateFilterButtons(2)
            viewModel.setFilterType(SearchViewModel.FilterType.TV_SHOWS)
        }

        // Sort by rating
        binding.btnSortRating.setOnClickListener {
            val isActive = binding.btnSortRating.isSelected
            binding.btnSortRating.isSelected = !isActive
            binding.btnSortPopularity.isSelected = false
            viewModel.setSortByRating(!isActive)
            viewModel.setSortByPopularity(false)
        }

        // Sort by popularity
        binding.btnSortPopularity.setOnClickListener {
            val isActive = binding.btnSortPopularity.isSelected
            binding.btnSortPopularity.isSelected = !isActive
            binding.btnSortRating.isSelected = false
            viewModel.setSortByPopularity(!isActive)
            viewModel.setSortByRating(false)
        }

        binding.btnResetFilters.setOnClickListener {
            viewModel.resetFilters()
            binding.sliderMinRating.value = 0f
            binding.btnSortRating.isSelected = false
            binding.btnSortPopularity.isSelected = false
            Toast.makeText(this, getString(R.string.reset_filters), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFilterButtons(selected: Int) {
        binding.btnFilterAll.isSelected = selected == 0
        binding.btnFilterMovies.isSelected = selected == 1
        binding.btnFilterTv.isSelected = selected == 2
    }

    private fun updateActiveFiltersLabel() {
        val type = viewModel.filterType.value
        val sort = viewModel.sortByRating.value ?: false
        val sortPop = viewModel.sortByPopularity.value ?: false
        val minR = viewModel.minRating.value ?: 0.0
        val parts = mutableListOf<String>()
        if (type != null && type != SearchViewModel.FilterType.ALL) {
            parts.add(
                when (type) {
                    SearchViewModel.FilterType.MOVIES -> getString(R.string.filter_movies)
                    SearchViewModel.FilterType.TV_SHOWS -> getString(R.string.filter_tv_shows)
                    else -> ""
                }
            )
        }
        if (sort) parts.add("Сортовано за рейтингом")
        if (sortPop) parts.add("Сортовано за популярністю")
        if (minR > 0.0) parts.add("Рейтинг ≥ ${String.format(Locale.US, "%.1f", minR)}")
        binding.tvActiveFilters.text = if (parts.isEmpty()) {
            "Фільтри: немає"
        } else {
            "Фільтри: " + parts.joinToString(", ")
        }
    }

    private fun observeViewModel() {
        viewModel.searchResult.observe(this) { items ->
            if (items.isNullOrEmpty()) {
                binding.emptyLayout.visibility = View.VISIBLE
                binding.rvSearchResults.visibility = View.GONE
                binding.tvResultCount.text = getString(R.string.no_results)
            } else {
                binding.emptyLayout.visibility = View.GONE
                binding.rvSearchResults.visibility = View.VISIBLE
                val count = items.size
                binding.tvResultCount.text = "Знайдено: $count"
            }
            searchAdapter.submitList(items)
            searchAdapter.updateQueryHighlight(binding.etSearchQuery.text?.toString().orEmpty())
            updateActiveFiltersLabel()
        }

        viewModel.minRating.observe(this) { value ->
            binding.tvMinRatingLabel.text = getString(R.string.rating_value, value)
            updateActiveFiltersLabel()
        }

        viewModel.searchHistory.observe(this) { history ->
            val container = binding.historyContainer
            val chipsHolder = binding.historyChips
            chipsHolder.removeAllViews()
            if (history.isEmpty()) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                history.forEach { q ->
                    val chip = Chip(this)
                    chip.text = q
                    chip.isCheckable = false
                    chip.isClickable = true
                    chip.setOnClickListener {
                        binding.etSearchQuery.setText(q)
                        binding.etSearchQuery.setSelection(q.length)
                        viewModel.searchMulti(q)
                    }
                    chip.setOnLongClickListener {
                        // анімація видалення
                        chip.animate()
                            .alpha(0f)
                            .scaleX(0.7f)
                            .scaleY(0.7f)
                            .setDuration(180)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    viewModel.removeHistoryItem(q)
                                    Toast.makeText(this@SearchActivity, getString(R.string.history_item_removed), Toast.LENGTH_SHORT).show()
                                }
                            }).start()
                        true
                    }
                    chipsHolder.addView(chip)
                }
                binding.btnClearHistory.setOnClickListener {
                    viewModel.clearSearchHistory()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.filterType.observe(this) { _ ->
            updateActiveFiltersLabel()
        }
        viewModel.sortByRating.observe(this) { _ ->
            updateActiveFiltersLabel()
        }
        viewModel.sortByPopularity.observe(this) { _ ->
            updateActiveFiltersLabel()
        }
    }

    private fun setupSearch() {
        // Debounce input using coroutine job
        binding.etSearchQuery.addTextChangedListener { editable ->
            val text = editable?.toString() ?: ""
            searchAdapter.updateQueryHighlight(text)
            searchJob?.cancel()
            if (text.isBlank()) {
                // clear results immediately
                viewModel.searchMulti("")
                return@addTextChangedListener
            }

            // Мінімум 2 символи для пошуку
            if (text.length < 2) {
                return@addTextChangedListener
            }

            searchJob = lifecycleScope.launch {
                delay(400) // 400 ms debounce
                viewModel.searchMulti(text)
            }
        }

        // Also support IME search action
        binding.etSearchQuery.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString()
                if (query.isNotBlank() && query.length >= 2) {
                    searchJob?.cancel()
                    viewModel.searchMulti(query)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
}