package com.example.movietime.ui.search

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.ActivityEnhancedSearchBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class EnhancedSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnhancedSearchBinding
    private val viewModel: SearchViewModel by viewModels()

    private lateinit var searchAdapter: GroupedSearchAdapter
    private lateinit var popularAdapter: GroupedSearchAdapter

    private var searchJob: Job? = null
    private var currentFilter = "all"

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
        binding = ActivityEnhancedSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupSearchInput()
        setupTabs()
        observeViewModel()
        loadPopularContent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        searchAdapter = GroupedSearchAdapter { item ->
            navigateToDetails(item)
        }

        popularAdapter = GroupedSearchAdapter { item ->
            navigateToDetails(item)
        }

        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@EnhancedSearchActivity)
        }

        binding.rvPopular.apply {
            adapter = popularAdapter
            layoutManager = LinearLayoutManager(this@EnhancedSearchActivity)
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                performSearch(query)
            }
        })
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "all"
                    1 -> "movie"
                    2 -> "tv"
                    else -> "all"
                }
                val query = binding.etSearch.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()

        if (query.isEmpty()) {
            showPopularContent()
            return
        }

        if (query.length < 2) return

        searchJob = lifecycleScope.launch {
            delay(500) // Debounce search

            binding.layoutLoading.isVisible = true
            binding.cardResults.isVisible = false
            binding.cardPopular.isVisible = false

            when (currentFilter) {
                "movie" -> viewModel.searchMovies(query)
                "tv" -> viewModel.searchTvShows(query)
                else -> viewModel.searchAll(query)
            }
        }
    }

    private fun showPopularContent() {
        binding.cardResults.isVisible = false
        binding.cardPopular.isVisible = true
        binding.layoutLoading.isVisible = false
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
            binding.layoutLoading.isVisible = false

            if (results.isNotEmpty()) {
                val groupedItems = results.map { item ->
                    when (item) {
                        is MovieResult -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                        is TvShowResult -> GroupedSearchItem(GroupedSearchItem.ItemType.TV_SHOW, item)
                        else -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                    }
                }
                searchAdapter.updateItems(groupedItems)
                binding.cardResults.isVisible = true
                binding.cardPopular.isVisible = false

                val resultCount = results.size
                binding.tvResultsHeader.text = getString(R.string.search_results_count, resultCount)
            } else {
                binding.cardResults.isVisible = false
                showPopularContent()
            }
        }

        viewModel.popularContent.observe(this) { popular ->
            if (popular.isNotEmpty()) {
                val groupedItems = popular.map { item ->
                    when (item) {
                        is MovieResult -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                        is TvShowResult -> GroupedSearchItem(GroupedSearchItem.ItemType.TV_SHOW, item)
                        else -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                    }
                }
                popularAdapter.updateItems(groupedItems)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.layoutLoading.isVisible = isLoading
        }
    }

    private fun loadPopularContent() {
        viewModel.loadPopularContent()
    }

    private fun navigateToDetails(item: Any) {
        when (item) {
            is MovieResult -> {
                val intent = Intent(this, DetailsActivity::class.java).apply {
                    putExtra("MOVIE_ID", item.id)
                }
                startActivity(intent)
            }
            is TvShowResult -> {
                val intent = Intent(this, TvDetailsActivity::class.java).apply {
                    putExtra("TV_ID", item.id)
                }
                startActivity(intent)
            }
        }
    }
}
