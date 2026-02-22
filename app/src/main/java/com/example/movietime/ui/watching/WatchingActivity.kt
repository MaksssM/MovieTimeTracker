package com.example.movietime.ui.watching

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ActivityWatchingBinding
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.adapters.ContentAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class WatchingActivity : AppCompatActivity() {

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

    private lateinit var binding: ActivityWatchingBinding
    private val viewModel: WatchingViewModel by viewModels()

    private lateinit var watchingAdapter: ContentAdapter
    private var currentFilter = "all"
    private var currentSort = SortType.DATE_NEWEST
    private var searchQuery = ""

    enum class SortType {
        DATE_NEWEST, DATE_OLDEST,
        NAME_ASC, NAME_DESC,
        RATING_HIGH, RATING_LOW
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Activity transition
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.activity_open_enter, R.anim.smooth_fade_out)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupSortAndSearch()
        setupClickListeners()
        observeViewModel()
        loadWatchingContent()
        animateEntrance()
    }

    private fun animateEntrance() {
        // Animate FAB with bounce
        binding.fabAddWatching.alpha = 0f
        binding.fabAddWatching.scaleX = 0f
        binding.fabAddWatching.scaleY = 0f
        binding.fabAddWatching.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(400)
            .setInterpolator(OvershootInterpolator(2.5f))
            .start()

        // Animate search bar
        binding.etSearch?.let { search ->
            search.alpha = 0f
            search.translationY = 30f
            search.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Animate sort button
        binding.btnSort?.let { sort ->
            sort.alpha = 0f
            sort.scaleX = 0f
            sort.scaleY = 0f
            sort.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }

        // Animate toolbar
        binding.toolbar.alpha = 0f
        binding.toolbar.translationY = -20f
        binding.toolbar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.activity_close_exit)
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
                showMoveOptionsDialog(item)
            }
        )

        binding.rvWatching.apply {
            adapter = watchingAdapter
            layoutManager = LinearLayoutManager(this@WatchingActivity)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.layout_animation_slide_up
            )
        }
    }

    private fun showMoveOptionsDialog(item: com.example.movietime.data.db.WatchedItem) {
        val options = arrayOf(
            getString(R.string.move_to_watched),
            getString(R.string.move_to_planned),
            getString(R.string.delete)
        )
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.moveToWatched(item)
                        Toast.makeText(this, getString(R.string.moved_to_watched), Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        viewModel.moveToPlanned(item)
                        Toast.makeText(this, getString(R.string.moved_to_planned), Toast.LENGTH_SHORT).show()
                    }
                    2 -> viewModel.removeFromWatching(item)
                }
            }
            .show()
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

    private fun setupSortAndSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterContent()
            }
        })

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_date_newest),
            getString(R.string.sort_by_date_oldest),
            getString(R.string.sort_by_name_asc),
            getString(R.string.sort_by_name_desc),
            getString(R.string.sort_by_rating_high),
            getString(R.string.sort_by_rating_low)
        )

        val currentIndex = when (currentSort) {
            SortType.DATE_NEWEST -> 0
            SortType.DATE_OLDEST -> 1
            SortType.NAME_ASC -> 2
            SortType.NAME_DESC -> 3
            SortType.RATING_HIGH -> 4
            SortType.RATING_LOW -> 5
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                currentSort = when (which) {
                    0 -> SortType.DATE_NEWEST
                    1 -> SortType.DATE_OLDEST
                    2 -> SortType.NAME_ASC
                    3 -> SortType.NAME_DESC
                    4 -> SortType.RATING_HIGH
                    5 -> SortType.RATING_LOW
                    else -> SortType.DATE_NEWEST
                }
                filterContent()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applySorting(items: List<WatchedItem>): List<WatchedItem> {
        return when (currentSort) {
            SortType.DATE_NEWEST -> items.sortedByDescending { it.lastUpdated ?: 0L }
            SortType.DATE_OLDEST -> items.sortedBy { it.lastUpdated ?: 0L }
            SortType.NAME_ASC -> items.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
            SortType.RATING_HIGH -> items.sortedByDescending { it.voteAverage ?: 0.0 }
            SortType.RATING_LOW -> items.sortedBy { it.voteAverage ?: 0.0 }
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
            binding.rvWatching.isVisible = !isLoading && (viewModel.watchingContent.value?.isNotEmpty() == true)
            binding.layoutEmpty.isVisible = !isLoading && (viewModel.watchingContent.value?.isEmpty() == true)
        }
    }

    private fun loadWatchingContent() {
        viewModel.loadWatchingContent()
    }

    private fun filterContent() {
        val allContent = viewModel.watchingContent.value ?: emptyList()

        var filteredContent = when (currentFilter) {
            "movie" -> allContent.filter { it.mediaType == "movie" }
            "tv" -> allContent.filter { it.mediaType == "tv" }
            else -> allContent
        }

        // Apply search
        if (searchQuery.isNotEmpty()) {
            filteredContent = filteredContent.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sort
        filteredContent = applySorting(filteredContent)

        watchingAdapter.updateItems(filteredContent)

        if (filteredContent.isEmpty()) {
            binding.rvWatching.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvWatching.isVisible = true
            binding.layoutEmpty.isVisible = false
            // Re-run layout animation when filter changes
            binding.rvWatching.scheduleLayoutAnimation()
        }

        supportActionBar?.subtitle = when (currentFilter) {
            "movie" -> getString(R.string.watching_movies_count, filteredContent.size)
            "tv" -> getString(R.string.watching_tv_shows_count, filteredContent.size)
            else -> getString(R.string.watching_count, filteredContent.size)
        }
    }

    private fun showEmptyState() {
        binding.rvWatching.isVisible = false
        binding.layoutEmpty.isVisible = true
    }

    private fun showContent(@Suppress("UNUSED_PARAMETER") content: List<Any>) {
        binding.rvWatching.isVisible = true
        binding.layoutEmpty.isVisible = false
    }
}
