package com.example.movietime.ui.watching

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.ActivityWatchingBinding
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.adapters.ContentAdapter
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchingBinding
    private val viewModel: WatchingViewModel by viewModels()

    private lateinit var watchingAdapter: ContentAdapter
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupClickListeners()
        observeViewModel()
        loadWatchingContent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.nav_watching)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        watchingAdapter = ContentAdapter(
            onItemClick = { item ->
                // Navigate to details
                // TODO: Implement navigation to details
            },
            onDeleteClick = { item ->
                viewModel.removeFromWatching(item)
            }
        )

        binding.rvWatching.apply {
            adapter = watchingAdapter
            layoutManager = LinearLayoutManager(this@WatchingActivity)
        }
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
                filterContent()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.fabAddWatching.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }

        binding.btnBrowseContent.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.watchingContent.observe(this) { content ->
            binding.layoutLoading.isVisible = false

            if (content.isEmpty()) {
                showEmptyState()
            } else {
                showContent(content)
                filterContent()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.layoutLoading.isVisible = isLoading
            binding.cardWatchingList.isVisible = !isLoading
            binding.layoutEmpty.isVisible = false
        }
    }

    private fun loadWatchingContent() {
        viewModel.loadWatchingContent()
    }

    private fun filterContent() {
        val allContent = viewModel.watchingContent.value ?: emptyList()

        val filteredContent = when (currentFilter) {
            "movie" -> allContent.filter { it.mediaType == "movie" }
            "tv" -> allContent.filter { it.mediaType == "tv" }
            else -> allContent
        }

        watchingAdapter.updateItems(filteredContent)

        val headerText = when (currentFilter) {
            "movie" -> getString(R.string.watching_movies_count, filteredContent.size)
            "tv" -> getString(R.string.watching_tv_shows_count, filteredContent.size)
            else -> getString(R.string.watching_count, filteredContent.size)
        }

        binding.tvWatchingHeader.text = headerText
    }

    private fun showEmptyState() {
        binding.cardWatchingList.isVisible = false
        binding.layoutEmpty.isVisible = true
    }

    private fun showContent(@Suppress("UNUSED_PARAMETER") content: List<Any>) {
        binding.cardWatchingList.isVisible = true
        binding.layoutEmpty.isVisible = false
    }
}
