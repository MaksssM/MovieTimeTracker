package com.example.movietime.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
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
    
    private var tvShow: TvShowResult? = null
    private var tvShowId: Int = -1
    
    private lateinit var seasonAdapter: SeasonProgressAdapter
    private val seasons = mutableListOf<SeasonUiModel>()
    
    private var onProgressSaved: ((Int) -> Unit)? = null
    
    companion object {
        private const val TAG = "TvProgressBottomSheet"
        private const val ARG_TV_SHOW_ID = "tv_show_id"
        private const val API_KEY = "ed63a6a76ed6bb079c7d41bad3cbc342"
        
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
        // Select All / Deselect All
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
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
            dismiss()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveProgress()
        }
    }
    
    private fun loadTvShowData() {
        if (tvShowId == -1) {
            showError(getString(R.string.error_loading_data))
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Load TV show details
                val language = getContentLanguage()
                tvShow = withContext(Dispatchers.IO) {
                    tmdbApi.getTvShowDetails(tvShowId, API_KEY, language)
                }
                
                tvShow?.let { show ->
                    // Update UI with show info
                    updateShowInfo(show)
                    
                    // Load existing progress from database
                    val existingProgress = withContext(Dispatchers.IO) {
                        tvShowProgressDao.getProgressForShowSync(tvShowId)
                    }
                    val progressMap = existingProgress.associateBy { ep -> 
                        "${ep.seasonNumber}_${ep.episodeNumber}" 
                    }
                    
                    // Load seasons data
                    val seasonsList = show.seasons?.filter { 
                        // Include season 0 (specials) and regular seasons
                        (it.seasonNumber ?: -1) >= 0 
                    } ?: emptyList()
                    
                    // Load all seasons in parallel
                    val seasonDetails = withContext(Dispatchers.IO) {
                        seasonsList.map { season ->
                            async {
                                try {
                                    tmdbApi.getSeasonDetails(
                                        tvShowId, 
                                        season.seasonNumber ?: 0, 
                                        API_KEY, 
                                        language
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    
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
                    
                    // Update UI
                    withContext(Dispatchers.Main) {
                        seasonAdapter.submitList(seasons.toList())
                        updateSelectAllState()
                        updateTotalProgress()
                        showLoading(false)
                    }
                } ?: run {
                    showError(getString(R.string.error_loading_data))
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(getString(R.string.error_loading_seasons))
                }
            }
        }
    }
    
    private fun updateShowInfo(show: TvShowResult) {
        binding.apply {
            tvShowTitle.text = show.name ?: getString(R.string.no_title)
            
            val totalEpisodes = seasons.sumOf { it.totalCount }
            tvShowSubtitle.text = getString(R.string.total_episodes_format, 
                show.numberOfEpisodes ?: totalEpisodes
            )
            
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
        
        binding.cbSelectAll.setOnCheckedChangeListener(null)
        binding.cbSelectAll.isChecked = totalEpisodes > 0 && watchedEpisodes == totalEpisodes
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            seasons.forEach { season ->
                season.episodes.forEach { episode ->
                    episode.isWatched = isChecked
                }
            }
            seasonAdapter.notifyDataSetChanged()
            updateTotalProgress()
        }
    }
    
    private fun updateTotalProgress() {
        val totalEpisodes = seasons.sumOf { it.totalCount }
        val watchedEpisodes = seasons.sumOf { it.watchedCount }
        val watchedRuntime = seasons.sumOf { it.watchedRuntime }
        
        binding.tvShowSubtitle.text = if (watchedEpisodes > 0) {
            "${getString(R.string.watched_episodes_format, watchedEpisodes, totalEpisodes)} • ${Utils.formatMinutesToHoursAndMinutes(watchedRuntime)}"
        } else {
            getString(R.string.total_episodes_format, totalEpisodes)
        }
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
                    
                    // Update or create WatchedItem
                    val existingItem = watchedItemDao.getById(tvShowId, "tv")
                    
                    if (watchedEpisodes > 0) {
                        // Determine status
                        val isOngoing = show.status in listOf("Returning Series", "In Production")
                        
                        val watchedItem = WatchedItem(
                            id = tvShowId,
                            title = show.name ?: "",
                            posterPath = show.posterPath,
                            releaseDate = show.firstAirDate,
                            runtime = watchedRuntime,
                            mediaType = "tv",
                            overview = show.overview,
                            voteAverage = show.voteAverage.toDouble(),
                            episodeRuntime = if (totalEpisodes > 0) watchedRuntime / watchedEpisodes else 0,
                            totalEpisodes = totalEpisodes,
                            isOngoing = isOngoing,
                            status = show.status,
                            lastUpdated = currentTime
                        )
                        
                        // Always use insert with REPLACE strategy
                        watchedItemDao.insert(watchedItem)
                    } else if (existingItem != null) {
                        // Remove from watched if no episodes watched
                        watchedItemDao.deleteById(tvShowId, "tv")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val watchedRuntime = seasons.sumOf { it.watchedRuntime }
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.progress_saved),
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
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.rvSeasons.visibility = if (loading) View.GONE else View.VISIBLE
        binding.btnSave.isEnabled = !loading
    }
    
    private fun showError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        dismiss()
    }
    
    private fun getContentLanguage(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        return when (prefs.getString("pref_lang", "uk")) {
            "uk" -> "uk-UA"
            "ru" -> "ru-RU"
            "en" -> "en-US"
            else -> "uk-UA"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
