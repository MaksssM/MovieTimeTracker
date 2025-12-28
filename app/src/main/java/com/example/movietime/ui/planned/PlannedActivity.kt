package com.example.movietime.ui.planned

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.ActivityPlannedBinding
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.adapters.ContentAdapter
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlannedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlannedBinding
    private val viewModel: PlannedViewModel by viewModels()

    private lateinit var plannedAdapter: ContentAdapter
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlannedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Activity transition
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.activity_open_enter, R.anim.smooth_fade_out)

        val isMovie = intent.getBooleanExtra("isMovie", true)
        val title = if (isMovie) getString(R.string.planned_movies) else getString(R.string.planned_tv_shows)

        setupToolbar(title)
        setupRecyclerView()
        setupTabs()
        setupClickListeners()
        observeViewModel()
        loadPlannedContent()
    }
    
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.activity_close_exit)
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        plannedAdapter = ContentAdapter(
            onItemClick = { item ->
                // Navigate to details
                val intent = if (item.mediaType == "tv") {
                    Intent(this, com.example.movietime.ui.details.TvDetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.id)
                        putExtra("MEDIA_TYPE", "tv")
                    }
                } else {
                    Intent(this, com.example.movietime.ui.details.DetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.id)
                        putExtra("MEDIA_TYPE", "movie")
                    }
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                viewModel.removeFromPlanned(item)
            }
        )

        binding.rvPlanned.apply {
            adapter = plannedAdapter
            layoutManager = LinearLayoutManager(this@PlannedActivity)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.layout_animation_cascade
            )
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
        binding.fabAddPlanned.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }

        binding.btnBrowseContent.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.plannedContent.observe(this) { content ->
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
            binding.rvPlanned.isVisible = !isLoading && (viewModel.plannedContent.value?.isNotEmpty() == true)
            binding.layoutEmpty.isVisible = !isLoading && (viewModel.plannedContent.value?.isEmpty() == true)
        }
    }

    private fun loadPlannedContent() {
        viewModel.loadPlannedContent()
    }

    private fun filterContent() {
        val allContent = viewModel.plannedContent.value ?: emptyList()

        val filteredContent = when (currentFilter) {
            "movie" -> allContent.filter { it.mediaType == "movie" }
            "tv" -> allContent.filter { it.mediaType == "tv" }
            else -> allContent
        }

        plannedAdapter.updateItems(filteredContent)

        if (filteredContent.isEmpty()) {
            binding.rvPlanned.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvPlanned.isVisible = true
            binding.layoutEmpty.isVisible = false
            // Re-run layout animation when filter changes
            binding.rvPlanned.scheduleLayoutAnimation()
        }
        
        // Header text logic removed as it was part of the old card layout header which is now replaced by toolbar/tabs
        supportActionBar?.subtitle = when (currentFilter) {
            "movie" -> getString(R.string.planned_movies_count, filteredContent.size)
            "tv" -> getString(R.string.planned_tv_shows_count, filteredContent.size)
            else -> getString(R.string.planned_content_count, filteredContent.size)
        }
    }

    private fun showEmptyState() {
        binding.rvPlanned.isVisible = false
        binding.layoutEmpty.isVisible = true
    }

    private fun showContent(@Suppress("UNUSED_PARAMETER") content: List<Any>) {
        binding.rvPlanned.isVisible = true
        binding.layoutEmpty.isVisible = false
    }
}
