package com.example.movietime.ui.details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.movietime.BuildConfig
import com.example.movietime.R
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.TvShowProgress
import com.example.movietime.data.db.TvShowProgressDao
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.model.TvSeasonDetails
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.databinding.BottomSheetTvProgressBinding
import com.example.movietime.util.Utils
import com.example.movietime.util.LanguageManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TvProgressBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTvProgressBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var tmdbApi: TmdbApi
    
    @Inject
    lateinit var tvShowProgressDao: TvShowProgressDao
    
    @Inject
    lateinit var watchedItemDao: WatchedItemDao

    @Inject
    lateinit var languageManager: LanguageManager
    
    private var tvShow: TvShowResult? = null
    private var tvShowId: Int = -1
    
    private lateinit var seasonAdapter: SeasonProgressAdapter
    private val seasons = mutableListOf<SeasonUiModel>()
    
    private var onProgressSaved: ((Int) -> Unit)? = null
    
    companion object {
        private const val TAG = "TvProgressBottomSheet"
        private const val ARG_TV_SHOW_ID = "tv_show_id"
        private val API_KEY = BuildConfig.TMDB_API_KEY
        
        fun newInstance(tvShowId: Int, onProgressSaved: ((Int) -> Unit)? = null): TvProgressBottomSheet {
            return TvProgressBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TV_SHOW_ID, tvShowId)
                }
                this.onProgressSaved = onProgressSaved
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_MovieTime_BottomSheet)
        tvShowId = arguments?.getInt(ARG_TV_SHOW_ID, -1) ?: -1
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTvProgressBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBehavior()
        setupAdapter()
        setupClickListeners()
        loadTvShowData()
    }
    
    private fun setupBehavior() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                
                // Set max height to 90% of screen
                val screenHeight = resources.displayMetrics.heightPixels
                it.layoutParams.height = (screenHeight * 0.9).toInt()
                it.requestLayout()
            }
        }
    }
    
    private fun setupAdapter() {
        seasonAdapter = SeasonProgressAdapter(
            onSeasonChecked = { season, isChecked ->
                // All episodes in season were toggled
                updateSelectAllState()
                updateTotalProgress()
            },
            onEpisodeChecked = { season, episode, isChecked ->
                // Single episode was toggled
                updateSelectAllState()
                updateTotalProgress()
            }
        )
        binding.rvSeasons.adapter = seasonAdapter
    }
    
    private fun setupClickListeners() {
        // Select All / Deselect All - using Chip instead of CheckBox
        binding.chipSelectAll.setOnClickListener {
            com.example.movietime.utils.HapticFeedbackHelper.impactMedium(it)
            val isChecked = binding.chipSelectAll.isChecked
            seasons.forEach { season ->
                season.episodes.forEach { episode ->
                    episode.isWatched = isChecked
                }
            }
            seasonAdapter.notifyDataSetChanged()
            updateTotalProgress()
        }
        
        // Cancel button
        binding.btnCancel.setOnClickListener {
            com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
            dismiss()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            com.example.movietime.utils.HapticFeedbackHelper.impactMedium(it)
            saveProgress()
        }
        
        // Retry button
        binding.btnRetry.setOnClickListener {
            com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
            loadTvShowData()
        }
    }
    
    private fun loadTvShowData() {
        if (tvShowId == -1) {
            Log.e(TAG, "Invalid tvShowId: $tvShowId")
            showError(getString(R.string.error_loading_data))
            return
        }
        
        Log.d(TAG, "Loading TV show data for ID: $tvShowId")
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Load TV show details
                val language = languageManager.getApiLanguage()
                Log.d(TAG, "Fetching TV show details with language: $language")
                
                tvShow = withContext(Dispatchers.IO) {
                    tmdbApi.getTvShowDetails(tvShowId, API_KEY, language)
                }
                
                Log.d(TAG, "TV show loaded: ${tvShow?.name}, seasons: ${tvShow?.seasons?.size}")
                
                tvShow?.let { show ->
                    // Update UI with show info
                    updateShowInfo(show)
                    
                    // Load existing progress from database
                    val existingProgress = withContext(Dispatchers.IO) {
                        tvShowProgressDao.getProgressForShowSync(tvShowId)
                    }
                    Log.d(TAG, "Existing progress: ${existingProgress.size} episodes")
                    
                    val progressMap = existingProgress.associateBy { ep -> 
                        "${ep.seasonNumber}_${ep.episodeNumber}" 
                    }
                    
                    // Load seasons data
                    val seasonsList = show.seasons?.filter { 
                        // Include season 0 (specials) and regular seasons
                        (it.seasonNumber ?: -1) >= 0 
                    } ?: emptyList()
                    
                    Log.d(TAG, "Loading ${seasonsList.size} seasons")
                    
                    // Load all seasons in parallel
                    val seasonDetails = withContext(Dispatchers.IO) {
                        seasonsList.map { season ->
                            async {
                                try {
                                    val seasonNum = season.seasonNumber ?: 0
                                    Log.d(TAG, "Fetching season $seasonNum")
                                    tmdbApi.getSeasonDetails(
                                        tvShowId, 
                                        seasonNum, 
                                        API_KEY, 
                                        language
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading season ${season.seasonNumber}: ${e.message}", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    
                    Log.d(TAG, "Successfully loaded ${seasonDetails.size} seasons")
                    
                    // Convert to UI models
                    seasons.clear()
                    seasonDetails.forEach { seasonDetail ->
                        val seasonNumber = seasonDetail.seasonNumber ?: 0
                        val episodes = seasonDetail.episodes?.map { episode ->
                            val key = "${seasonNumber}_${episode.episodeNumber}"
                            val isWatched = progressMap[key]?.watched ?: false
                            
                            EpisodeUiModel(
                                episodeNumber = episode.episodeNumber ?: 0,
                                seasonNumber = seasonNumber,
                                name = episode.name ?: getString(R.string.unknown_episode),
                                runtime = episode.runtime,
                                isWatched = isWatched
                            )
                        }?.toMutableList() ?: mutableListOf()
                        
                        seasons.add(
                            SeasonUiModel(
                                seasonNumber = seasonNumber,
                                name = seasonDetail.name ?: getString(R.string.season_format, seasonNumber),
                                episodes = episodes,
                                isExpanded = false
                            )
                        )
                    }
                    
                    // Sort: season 0 last if exists, then by number
                    seasons.sortWith(compareBy { if (it.seasonNumber == 0) Int.MAX_VALUE else it.seasonNumber })
                    
                    Log.d(TAG, "Total episodes across all seasons: ${seasons.sumOf { it.totalCount }}")
                    
                    // Update UI
                    withContext(Dispatchers.Main) {
                        seasonAdapter.submitList(seasons.toList())
                        updateSelectAllState()
                        updateTotalProgress()
                        showLoading(false)
                    }
                } ?: run {
                    Log.e(TAG, "TV show data is null")
                    showError(getString(R.string.error_loading_data))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading TV show data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showError("${getString(R.string.error_loading_seasons)}\n${e.message}")
                }
            }
        }
    }
    
    private fun updateShowInfo(show: TvShowResult) {
        binding.apply {
            tvShowTitle.text = show.name ?: getString(R.string.no_title)
            
            // Show seasons and episodes count
            val seasonsCount = show.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.size ?: 0
            val episodesCount = show.numberOfEpisodes ?: 0
            tvShowSubtitle.text = getString(R.string.seasons_episodes_format, seasonsCount, episodesCount)
            
            // Calculate total runtime estimate
            val avgEpisodeRuntime = show.episodeRunTime?.firstOrNull() ?: 45
            val totalMinutes = episodesCount * avgEpisodeRuntime
            tvTotalRuntime.text = getString(R.string.total_runtime_format, 
                Utils.formatMinutesToHoursAndMinutes(totalMinutes))
            
            val posterUrl = show.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" }
            ivShowPoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
        }
    }
    
    private fun updateSelectAllState() {
        val totalEpisodes = seasons.sumOf { it.totalCount }
        val watchedEpisodes = seasons.sumOf { it.watchedCount }
        
        // Update the chip state
        binding.chipSelectAll.isChecked = totalEpisodes > 0 && watchedEpisodes == totalEpisodes
        
        // Update progress text
        binding.tvTotalProgress.text = getString(R.string.selected_episodes_count, watchedEpisodes, totalEpisodes)
        
        // Update progress bar
        val progressPercent = if (totalEpisodes > 0) (watchedEpisodes * 100 / totalEpisodes) else 0
        binding.progressTotal.setProgressCompat(progressPercent, true)
        
        // Update stats row
        updateStatsRow()
    }
    
    private fun updateTotalProgress() {
        val totalEpisodes = seasons.sumOf { it.totalCount }
        val watchedEpisodes = seasons.sumOf { it.watchedCount }
        val watchedRuntime = seasons.sumOf { it.watchedRuntime }
        val isFullyWatched = totalEpisodes > 0 && watchedEpisodes >= totalEpisodes
        val isOngoing = tvShow?.status in listOf("Returning Series", "In Production")
        
        binding.tvShowSubtitle.text = if (watchedEpisodes > 0) {
            val progressText = when {
                // All episodes watched, series ended
                isFullyWatched && !isOngoing -> "✅ ${getString(R.string.watched_episodes_format, watchedEpisodes, totalEpisodes)}"
                // All current episodes watched, but series still ongoing
                isFullyWatched && isOngoing -> "⏸️ ${getString(R.string.watched_episodes_format, watchedEpisodes, totalEpisodes)}"
                // Partial progress
                else -> getString(R.string.watched_episodes_format, watchedEpisodes, totalEpisodes)
            }
            "$progressText • ${Utils.formatMinutesToHoursAndMinutes(watchedRuntime)}"
        } else {
            tvShow?.let { show ->
                val seasonsCount = show.seasons?.size ?: 0
                val episodesCount = show.numberOfEpisodes ?: totalEpisodes
                getString(R.string.seasons_episodes_format, seasonsCount, episodesCount)
            } ?: getString(R.string.total_episodes_format, totalEpisodes)
        }
        
        // Update select all state
        updateSelectAllState()
    }
    
    private fun updateStatsRow() {
        val totalEpisodes = seasons.sumOf { it.totalCount }
        val watchedEpisodes = seasons.sumOf { it.watchedCount }
        val watchedRuntime = seasons.sumOf { it.watchedRuntime }
        val remainingEpisodes = totalEpisodes - watchedEpisodes
        
        binding.statsRow.visibility = if (totalEpisodes > 0) View.VISIBLE else View.GONE
        
        binding.tvWatchedCount.text = watchedEpisodes.toString()
        binding.tvRemainingCount.text = remainingEpisodes.toString()
        
        // Format time with hours
        val hours = watchedRuntime / 60
        binding.tvWatchedTime.text = if (hours > 0) "${hours}г" else "${watchedRuntime}хв"
    }
    
    private fun saveProgress() {
        val show = tvShow ?: return
        
        binding.btnSave.isEnabled = false
        binding.progressLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Clear old progress
                    tvShowProgressDao.deleteProgressForShow(tvShowId)
                    
                    // Save new progress
                    val currentTime = System.currentTimeMillis()
                    val progressList = mutableListOf<TvShowProgress>()
                    
                    seasons.forEach { season ->
                        season.episodes.forEach { episode ->
                            progressList.add(
                                TvShowProgress(
                                    tvShowId = tvShowId,
                                    seasonNumber = season.seasonNumber,
                                    episodeNumber = episode.episodeNumber,
                                    episodeName = episode.name,
                                    episodeRuntime = episode.runtime ?: 0,
                                    watched = episode.isWatched,
                                    watchedAt = if (episode.isWatched) currentTime else null
                                )
                            )
                        }
                    }
                    
                    tvShowProgressDao.insertEpisodes(progressList)
                    
                    // Calculate total watched runtime
                    val watchedRuntime = seasons.sumOf { it.watchedRuntime }
                    val watchedEpisodes = seasons.sumOf { it.watchedCount }
                    val totalEpisodes = seasons.sumOf { it.totalCount }
                    
                    // Determine if series is fully watched (all available episodes)
                    val isFullyWatched = totalEpisodes > 0 && watchedEpisodes >= totalEpisodes
                    
                    // Determine if series is ongoing (still releasing new episodes)
                    val isOngoing = show.status in listOf("Returning Series", "In Production")
                    
                    // Determine final status for the series
                    val finalStatus = when {
                        // If all episodes watched AND series is ended (not ongoing)
                        isFullyWatched && !isOngoing -> "Ended"
                        // If all current episodes watched BUT series is still ongoing
                        isFullyWatched && isOngoing -> "Returning Series" 
                        // Otherwise keep original status
                        else -> show.status
                    }
                    
                    // Update or create WatchedItem
                    val existingItem = watchedItemDao.getById(tvShowId, "tv")
                    
                    if (watchedEpisodes > 0) {
                        val watchedItem = WatchedItem(
                            id = tvShowId,
                            title = show.name ?: "",
                            posterPath = show.posterPath,
                            releaseDate = show.firstAirDate,
                            runtime = watchedRuntime,
                            mediaType = "tv",
                            overview = show.overview,
                            voteAverage = show.voteAverage.toDouble(),
                            episodeRuntime = if (watchedEpisodes > 0) watchedRuntime / watchedEpisodes else 0,
                            totalEpisodes = totalEpisodes,
                            isOngoing = isOngoing, // Keep original ongoing status
                            status = finalStatus,
                            lastUpdated = currentTime
                        )
                        
                        // Always use insert with REPLACE strategy
                        watchedItemDao.insert(watchedItem)
                        
                        Log.d(TAG, "Series saved - Progress: $watchedEpisodes/$totalEpisodes, isFullyWatched: $isFullyWatched, isOngoing: $isOngoing, finalStatus: $finalStatus")
                    } else {
                        // Remove from watched if no episodes watched and item exists
                        existingItem?.let {
                            watchedItemDao.deleteById(tvShowId, "tv")
                            Log.d(TAG, "Removed series from watched (no episodes marked)")
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val watchedRuntime = seasons.sumOf { it.watchedRuntime }
                    val watchedEpisodes = seasons.sumOf { it.watchedCount }
                    val totalEpisodes = seasons.sumOf { it.totalCount }
                    val isFullyWatched = totalEpisodes > 0 && watchedEpisodes >= totalEpisodes
                    val isOngoing = tvShow?.status in listOf("Returning Series", "In Production")
                    
                    val message = when {
                        // All episodes watched, series ended
                        isFullyWatched && !isOngoing -> getString(R.string.series_completed_message)
                        // All current episodes watched, but series still ongoing
                        isFullyWatched && isOngoing -> getString(R.string.series_up_to_date_message)
                        // Partial progress or no episodes
                        else -> getString(R.string.progress_saved)
                    }
                    
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                    onProgressSaved?.invoke(watchedRuntime)
                    dismiss()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showLoading(loading: Boolean) {
        binding.loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
        binding.rvSeasons.visibility = if (loading) View.GONE else View.VISIBLE
        binding.errorContainer.visibility = View.GONE
        binding.btnSave.isEnabled = !loading
    }
    
    private fun showError(message: String) {
        binding.loadingContainer.visibility = View.GONE
        binding.rvSeasons.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.btnSave.isEnabled = false
    }
    
    private fun getContentLanguage(): String =
        languageManager.getApiLanguage()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
