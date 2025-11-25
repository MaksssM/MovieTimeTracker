package com.example.movietime.ui.base

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.ActivityContentListBinding
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.adapters.ContentAdapter
import com.google.android.material.tabs.TabLayout
import java.util.Locale

/**
 * Base Activity for content list screens (Planned, Watching)
 * Reduces code duplication and provides common functionality
 */
abstract class BaseContentActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityContentListBinding
    protected lateinit var contentAdapter: ContentAdapter
    protected var currentFilter = "all"

    abstract val titleResId: Int
    abstract val emptyStateTextResId: Int
    abstract val headerTextResId: Int

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
        binding = ActivityContentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupClickListeners()
        observeViewModel()
        loadContent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(titleResId)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        contentAdapter = ContentAdapter(
            onItemClick = { item -> onItemClicked(item) },
            onDeleteClick = { item -> onItemDeleted(item) }
        )

        binding.rvContent.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(this@BaseContentActivity)
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
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }

        binding.btnBrowseContent.setOnClickListener {
            startActivity(Intent(this, EnhancedSearchActivity::class.java))
        }
    }

    protected fun showLoading() {
        binding.layoutLoading.isVisible = true
        binding.cardContentList.isVisible = false
        binding.layoutEmpty.isVisible = false
    }

    protected fun showContent() {
        binding.layoutLoading.isVisible = false
        binding.cardContentList.isVisible = true
        binding.layoutEmpty.isVisible = false
    }

    protected fun showEmptyState() {
        binding.layoutLoading.isVisible = false
        binding.cardContentList.isVisible = false
        binding.layoutEmpty.isVisible = true
    }

    protected fun updateHeader(count: Int) {
        binding.tvContentHeader.text = getString(headerTextResId, count)
    }

    protected fun filterContent() {
        val allContent = getAllContent()

        val filteredContent = when (currentFilter) {
            "movie" -> allContent.filter { it.mediaType == "movie" }
            "tv" -> allContent.filter { it.mediaType == "tv" }
            else -> allContent
        }

        contentAdapter.updateItems(filteredContent)
        updateHeader(filteredContent.size)
    }

    abstract fun observeViewModel()
    abstract fun loadContent()
    abstract fun getAllContent(): List<com.example.movietime.data.db.WatchedItem>
    abstract fun onItemClicked(item: com.example.movietime.data.db.WatchedItem)
    abstract fun onItemDeleted(item: com.example.movietime.data.db.WatchedItem)
}

