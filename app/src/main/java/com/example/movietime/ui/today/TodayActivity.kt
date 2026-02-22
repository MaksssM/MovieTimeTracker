package com.example.movietime.ui.today

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.data.model.*
import com.example.movietime.databinding.ActivityTodayBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.ui.today.adapters.*
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TodayActivity : AppCompatActivity() {

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

    private lateinit var binding: ActivityTodayBinding
    private val viewModel: TodayViewModel by viewModels()

    private lateinit var continueWatchingAdapter: ContinueWatchingAdapter
    private lateinit var newEpisodeAdapter: NewEpisodeAdapter
    private lateinit var releasesAdapter: TodayReleaseAdapter
    private lateinit var upcomingAdapter: TodayReleaseAdapter
    private lateinit var tipsAdapter: PersonalTipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupAdapters()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

        // Set greeting based on time
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when (hour) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..17 -> getString(R.string.good_afternoon)
            in 18..22 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent)
    }

    private fun setupAdapters() {
        // Continue Watching Adapter
        continueWatchingAdapter = ContinueWatchingAdapter { item ->
            openContent(item.id, item.mediaType)
        }
        binding.rvContinueWatching.apply {
            adapter = continueWatchingAdapter
            layoutManager = LinearLayoutManager(this@TodayActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // New Episodes Adapter
        newEpisodeAdapter = NewEpisodeAdapter { item ->
            openTvShow(item.tvShowId, item.seasonNumber, item.episodeNumber)
        }
        binding.rvNewEpisodes.apply {
            adapter = newEpisodeAdapter
            layoutManager = LinearLayoutManager(this@TodayActivity)
            isNestedScrollingEnabled = false
        }

        // Today's Releases Adapter
        releasesAdapter = TodayReleaseAdapter { item ->
            openContent(item.id, item.mediaType)
        }
        binding.rvReleases.apply {
            adapter = releasesAdapter
            layoutManager = LinearLayoutManager(this@TodayActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Upcoming Adapter
        upcomingAdapter = TodayReleaseAdapter { item ->
            openContent(item.id, item.mediaType)
        }
        binding.rvUpcoming.apply {
            adapter = upcomingAdapter
            layoutManager = LinearLayoutManager(this@TodayActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Personal Tips Adapter
        tipsAdapter = PersonalTipAdapter { tip ->
            handleTipAction(tip)
        }
        binding.rvTips.apply {
            adapter = tipsAdapter
            layoutManager = LinearLayoutManager(this@TodayActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshData()
        }

        // Stats cards click - scroll to section
        binding.cardNewEpisodes.setOnClickListener {
            scrollToSection(binding.sectionNewEpisodes)
        }
        binding.cardReleases.setOnClickListener {
            scrollToSection(binding.sectionReleases)
        }
        binding.cardUpcoming.setOnClickListener {
            scrollToSection(binding.sectionUpcoming)
        }

        binding.btnSeeAllUpcoming.setOnClickListener {
            // Navigate to full upcoming list
            // Intent to UpcomingReleasesActivity
        }
    }

    private fun scrollToSection(section: View) {
        val scrollView = binding.swipeRefresh.getChildAt(0) as? androidx.core.widget.NestedScrollView
        scrollView?.smoothScrollTo(0, section.top)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading && viewModel.digest.value == null
            binding.swipeRefresh.isRefreshing = isLoading && viewModel.digest.value != null
        }

        viewModel.error.observe(this) { error ->
            // Handle error if needed
        }

        viewModel.digest.observe(this) { digest ->
            digest?.let { updateDigest(it) }
        }

        viewModel.newEpisodesCount.observe(this) { count ->
            binding.tvNewEpisodesCount.text = count.toString()
            binding.tvNewEpisodesBadge.text = resources.getQuantityString(R.plurals.new_items_count, count, count)
        }

        viewModel.releasesCount.observe(this) { count ->
            binding.tvReleasesCount.text = count.toString()
            binding.tvReleasesBadge.text = resources.getQuantityString(R.plurals.releases_count_plurals, count, count)
        }

        viewModel.upcomingCount.observe(this) { count ->
            binding.tvUpcomingCount.text = count.toString()
        }

        viewModel.continueWatching.observe(this) { items ->
            binding.sectionContinueWatching.isVisible = items.isNotEmpty()
            continueWatchingAdapter.submitList(items)
        }

        viewModel.filteredNewEpisodes.observe(this) { episodes ->
            binding.sectionNewEpisodes.isVisible = episodes.isNotEmpty()
            newEpisodeAdapter.submitList(episodes)
        }

        viewModel.filteredReleases.observe(this) { releases ->
            binding.sectionReleases.isVisible = releases.isNotEmpty()
            releasesAdapter.submitList(releases)
        }

        viewModel.filteredUpcoming.observe(this) { upcoming ->
            binding.sectionUpcoming.isVisible = upcoming.isNotEmpty()
            upcomingAdapter.submitList(upcoming)
        }

        viewModel.personalTips.observe(this) { tips ->
            binding.sectionTips.isVisible = tips.isNotEmpty()
            tipsAdapter.submitList(tips)
        }
    }

    private fun updateDigest(digest: TodayDigest) {
        // Check if all content is empty
        val isEmpty = digest.newEpisodes.isEmpty() && 
                      digest.todayReleases.isEmpty() && 
                      digest.upcomingThisWeek.isEmpty() &&
                      digest.continueWatching.isEmpty()
        
        binding.emptyState.isVisible = isEmpty
    }

    private fun openContent(id: Int, mediaType: String) {
        val intent = if (mediaType == "tv") {
            Intent(this, TvDetailsActivity::class.java).apply {
                putExtra("ITEM_ID", id)
                putExtra("MEDIA_TYPE", "tv")
            }
        } else {
            Intent(this, DetailsActivity::class.java).apply {
                putExtra("ITEM_ID", id)
                putExtra("MEDIA_TYPE", "movie")
            }
        }
        startActivity(intent)
    }

    private fun openTvShow(tvShowId: Int, seasonNumber: Int = 1, episodeNumber: Int = 1) {
        val intent = Intent(this, TvDetailsActivity::class.java).apply {
            putExtra("ITEM_ID", tvShowId)
            putExtra("MEDIA_TYPE", "tv")
            putExtra("SEASON_NUMBER", seasonNumber)
            putExtra("EPISODE_NUMBER", episodeNumber)
        }
        startActivity(intent)
    }

    private fun handleTipAction(tip: PersonalTip) {
        when (tip.type) {
            TipType.CONTINUE_WATCHING, TipType.REWATCH_SUGGESTION -> {
                tip.relatedItemId?.let { id ->
                    openContent(id, tip.relatedItemType ?: "movie")
                }
            }
            TipType.NEW_SEASON_AVAILABLE, TipType.UPCOMING_REMINDER -> {
                tip.relatedItemId?.let { id ->
                    openTvShow(id)
                }
            }
            TipType.SIMILAR_CONTENT -> {
                tip.relatedItemId?.let { id ->
                    openContent(id, tip.relatedItemType ?: "movie")
                }
            }
            TipType.MILESTONE_REACHED -> {
                // Navigate to statistics
            }
        }
    }
}
