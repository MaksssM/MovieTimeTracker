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
import com.example.movietime.data.model.Person
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Locale
import com.example.movietime.ui.person.PersonDetailsActivity

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
        
        // Smooth window transitions
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.activity_open_enter, R.anim.smooth_fade_out)
        
        binding = ActivityEnhancedSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupSearchInput()
        setupTabs()
        observeViewModel()
        loadPopularContent()
    }
    
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.activity_close_exit)
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
            val layoutManager = LinearLayoutManager(this@EnhancedSearchActivity)
            this.layoutManager = layoutManager
            layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(
                context, 
                R.anim.layout_animation_cascade
            )
            
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) { // Scrolling down
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                            loadMore()
                        }
                    }
                }
            })
        }

        binding.rvPopular.apply {
            adapter = popularAdapter
            layoutManager = LinearLayoutManager(this@EnhancedSearchActivity)
            layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.layout_animation_fall_down
            )
        }
    }

    private fun loadMore() {
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        if (query.length < 2) return

        lifecycleScope.launch {
            if (currentFilter == "person") {
                viewModel.searchPeopleOnly(query, true)
            } else {
                // For other types, searchMulti respects the current filter set in ViewModel
                viewModel.searchMulti(query, true)
            }
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
                    3 -> "person"
                    else -> "all"
                }
                val query = binding.etSearch.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    updatePopularContent()
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
                "person" -> viewModel.searchPeopleOnly(query)
                else -> viewModel.searchAll(query)
            }
        }
    }

    private fun showPopularContent() {
        binding.cardResults.isVisible = false
        binding.cardPopular.isVisible = true
        binding.layoutLoading.isVisible = false
    }

    private fun updatePopularContent() {
        val allPopular = viewModel.popularContent.value ?: emptyList()
        if (allPopular.isEmpty()) return

        val filtered = when (currentFilter) {
            "movie" -> allPopular.filterIsInstance<MovieResult>()
            "tv" -> allPopular.filterIsInstance<TvShowResult>()
            else -> allPopular
        }

        if (filtered.isNotEmpty()) {
            val groupedItems = filtered.map { item ->
                when (item) {
                    is MovieResult -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                    is TvShowResult -> GroupedSearchItem(GroupedSearchItem.ItemType.TV_SHOW, item)
                    is Person -> GroupedSearchItem(GroupedSearchItem.ItemType.PERSON, item)
                    else -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                }
            }
            popularAdapter.updateItems(groupedItems)
        } else {
             popularAdapter.updateItems(emptyList())
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
            binding.layoutLoading.isVisible = false

            if (results.isNotEmpty()) {
                val groupedItems = results.map { item ->
                    when (item) {
                        is MovieResult -> GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, item)
                        is TvShowResult -> GroupedSearchItem(GroupedSearchItem.ItemType.TV_SHOW, item)
                        is Person -> GroupedSearchItem(GroupedSearchItem.ItemType.PERSON, item)
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

        viewModel.popularContent.observe(this) { 
            updatePopularContent()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.layoutLoading.isVisible = isLoading
        }
    }

    private fun loadPopularContent() {
        viewModel.loadPopularContent()
    }

    private fun navigateToDetails(item: Any) {
        Log.d("EnhancedSearchActivity", "Navigating to details for item: $item")
        when (item) {
            is MovieResult -> {
                Log.d("EnhancedSearchActivity", "Opening movie details: id=${item.id}, title=${item.title}")
                // Save to search history
                viewModel.addMovieToSearchHistory(item)
                val intent = Intent(this, DetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "movie")
                }
                startActivity(intent)
            }
            is TvShowResult -> {
                Log.d("EnhancedSearchActivity", "Opening TV show details: id=${item.id}, name=${item.name}")
                // Save to search history
                viewModel.addTvShowToSearchHistory(item)
                val intent = Intent(this, TvDetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "tv")
                }
                startActivity(intent)
            }
            is Person -> {
                Log.d("EnhancedSearchActivity", "Opening person details: id=${item.id}, name=${item.name}")
                val intent = Intent(this, PersonDetailsActivity::class.java).apply {
                    putExtra("PERSON_ID", item.id)
                }
                startActivity(intent)
            }
        }
    }
}

