package com.example.movietime.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.movietime.R
import com.example.movietime.databinding.FragmentEnhancedMainBinding
import com.example.movietime.data.model.BasicStatistics
import com.example.movietime.data.db.TvShowProgress
import com.example.movietime.data.db.TvShowProgressDao
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchingItem
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.data.model.RecentActivityItem
import dagger.hilt.android.AndroidEntryPoint
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@AndroidEntryPoint
class EnhancedMainFragment : Fragment() {

    private var _binding: FragmentEnhancedMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EnhancedMainViewModel by viewModels()
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private lateinit var recommendationsAdapter: com.example.movietime.ui.adapters.RecommendationsAdapter
    private lateinit var continueWatchingAdapter: com.example.movietime.ui.today.adapters.ContinueWatchingAdapter

    @javax.inject.Inject
    lateinit var appRepository: AppRepository

    @javax.inject.Inject
    lateinit var tvShowProgressDao: TvShowProgressDao

    private data class NextEpisodeToWatch(
        val show: WatchingItem,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val episodeName: String?,
        val watchedEpisodes: Int,
        val totalEpisodes: Int,
        val hasDetailedProgress: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnhancedMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Postpone transition until layout is ready
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        setupClickListeners()
        setupCardPressEffects()
        setupRecentActivity()
        setupRecommendations()
        setupContinueWatching()
        setupParallaxEffect()
        observeViewModel()
        loadData()
        animateEntranceElements()
    }

    private var lastClickTime = 0L
    private val clickDebounceTime = 500L // 500ms debounce
    private var lastWatchedCount = -1
    private var lastPlannedCount = -1
    private var lastWatchingCount = -1

    private fun setupClickListeners() {
        // Category cards - unified
        binding.cardWatched.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.watchedFragment)
            }
        }

        binding.cardPlanned.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.plannedFragment)
            }
        }

        binding.cardWatching.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.watchingFragment)
            }
        }

        // Header quick search
        binding.btnHeaderSearch.setOnClickListener {
            handleClickWithDebounce {
                startActivity(Intent(requireActivity(), EnhancedSearchActivity::class.java))
            }
        }

        // Quick action buttons
        binding.btnSearchMovies.setOnClickListener {
            handleClickWithDebounce {
                startActivity(Intent(requireActivity(), EnhancedSearchActivity::class.java))
            }
        }

        binding.btnTrending.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.trendingFragment)
            }
        }

        binding.btnUpcomingReleases.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.calendarFragment)
            }
        }

        binding.btnCollections.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.collectionsFragment)
            }
        }


        binding.btnFriends.setOnClickListener {
            handleClickWithDebounce {
                // Friends backend is down — show a stub instead of crashing
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.friends_coming_soon_title)
                    .setMessage(R.string.friends_coming_soon_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        binding.btnStatistics.setOnClickListener {
            handleClickWithDebounce {
                startActivity(Intent(requireActivity(), com.example.movietime.ui.statistics.StatisticsActivity::class.java))
            }
        }

        // See all recent activity - navigates to watched list
        binding.btnSeeAllActivity.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.watchedFragment)
            }
        }

        // Floating Action Button
        binding.fabAdd.setOnClickListener {
            handleClickWithDebounce {
                showQuickAddDialog()
            }
        }

        // Randomizer / What to Watch bottom sheet
        binding.btnRandomizer.setOnClickListener {
            handleClickWithDebounce {
                com.example.movietime.ui.randomizer.RandomizerBottomSheet.newInstance()
                    .show(childFragmentManager, com.example.movietime.ui.randomizer.RandomizerBottomSheet.TAG)
            }
        }
    }

    private fun handleClickWithDebounce(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > clickDebounceTime) {
            lastClickTime = currentTime
            action()
        }
    }

    private fun setupParallaxEffect() {
        // Parallax effect for floating orbs when scrolling
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val parallaxFactor = 0.4f
            val rotationFactor = 0.02f
            
            // Move floating orbs in opposite direction for parallax feel
            binding.floatingOrb1?.let { orb1 ->
                orb1.translationY = -scrollY * parallaxFactor
                orb1.translationX = scrollY * parallaxFactor * 0.3f
                orb1.rotation = scrollY * rotationFactor
                orb1.alpha = (0.5f - scrollY * 0.0003f).coerceIn(0.1f, 0.5f)
            }
            
            binding.floatingOrb2?.let { orb2 ->
                orb2.translationY = -scrollY * (parallaxFactor * 0.6f)
                orb2.translationX = -scrollY * parallaxFactor * 0.2f
                orb2.rotation = -scrollY * rotationFactor * 0.5f
                orb2.alpha = (0.35f - scrollY * 0.0002f).coerceIn(0.1f, 0.35f)
            }
            
            binding.floatingOrb3?.let { orb3 ->
                orb3.translationY = scrollY * (parallaxFactor * 0.3f)
                orb3.translationX = scrollY * parallaxFactor * 0.15f
                orb3.rotation = scrollY * rotationFactor * 0.3f
            }
            
            // Subtle scale effect on header
            binding.headerContainer?.let { header ->
                val scale = 1f - (scrollY * 0.0002f).coerceIn(0f, 0.1f)
                header.scaleX = scale
                header.scaleY = scale
                header.alpha = 1f - (scrollY * 0.001f).coerceIn(0f, 0.3f)
            }
        }
    }

    private fun setupRecommendations() {
        recommendationsAdapter = com.example.movietime.ui.adapters.RecommendationsAdapter { id, mediaType ->
            val intent = if (mediaType == "tv") {
                Intent(requireContext(), TvDetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", id)
                    putExtra("MEDIA_TYPE", "tv")
                }
            } else {
                Intent(requireContext(), DetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", id)
                    putExtra("MEDIA_TYPE", "movie")
                }
            }
            startActivity(intent)
        }

        binding.rvRecommendations.apply {
            adapter = recommendationsAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false).apply {
                initialPrefetchItemCount = 5
            }
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_scale_fade)
        }
    }

    // ...

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe statistics
            viewModel.getDetailedStatistics().collect { stats ->
                android.util.Log.d("EnhancedMainFragment", "Statistics received: totalWatchTimeMinutes=${stats.totalWatchTimeMinutes}, movies=${stats.totalWatchedMovies}, tv=${stats.totalWatchedTvShows}")
                updateStatistics(stats)
            }
        }

        // Observe recommendations
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendations.collect { recs ->
                if (recs.isNotEmpty()) {
                    binding.recommendationsTitleContainer.visibility = View.VISIBLE
                    binding.rvRecommendations.visibility = View.VISIBLE
                    recommendationsAdapter.submitList(recs)
                } else {
                    binding.recommendationsTitleContainer.visibility = View.GONE
                    binding.rvRecommendations.visibility = View.GONE
                }
            }
        }

        // Observe continue watching carousel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.continueWatching.collect { items ->
                if (items.isNotEmpty()) {
                    binding.layoutContinueWatching.visibility = View.VISIBLE
                    continueWatchingAdapter.submitList(items)
                } else {
                    binding.layoutContinueWatching.visibility = View.GONE
                }
            }
        }

        // Observe errors
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    // Don't show Snackbar for errors - just log them
                    android.util.Log.w("EnhancedMainFragment", "Error: $it")
                    viewModel.clearError()
                }
            }
        }

        // Observe recent activities
        viewLifecycleOwner.lifecycleScope.launch {
            android.util.Log.d("EnhancedMainFragment", "Starting recent activities observer...")
            viewModel.getRecentActivities().collect { activities ->
                android.util.Log.d("EnhancedMainFragment", "Recent activities received: ${activities.size} items")
                activities.forEach { item ->
                    android.util.Log.d("EnhancedMainFragment", "  - ${item.title} (${item.type}, ${item.mediaType})")
                }
                if (activities.isNotEmpty()) {
                    android.util.Log.d("EnhancedMainFragment", "Submitting ${activities.size} items to adapter")
                    binding.rvRecentActivity.visibility = View.VISIBLE
                    binding.emptyRecentActivity.visibility = View.GONE
                    recentActivityAdapter.submitList(activities)
                } else {
                    android.util.Log.d("EnhancedMainFragment", "No recent activities to display")
                    binding.rvRecentActivity.visibility = View.GONE
                    binding.emptyRecentActivity.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateStatistics(stats: BasicStatistics) {
        android.util.Log.d("EnhancedMainFragment", "updateStatistics called with: $stats")
        with(binding) {
            // Update header stats
            val formattedTime = formatTotalTime(stats.totalWatchTimeMinutes)
            android.util.Log.d("EnhancedMainFragment", "Formatted time: $formattedTime for ${stats.totalWatchTimeMinutes} minutes")
            tvTotalTime.text = formattedTime
            tvThisMonthCount.text = stats.thisMonthWatched.toString()
            
            // Update widget stats
            tvMoviesCountWidget.text = stats.totalWatchedMovies.toString()
            tvTvShowsCountWidget.text = stats.totalWatchedTvShows.toString()

            // Update quick stats
            tvWatchedMoviesCount.text = stats.totalWatchedMovies.toString()
            tvWatchedTvShowsCount.text = stats.totalWatchedTvShows.toString()
            tvAverageRating.text = if (stats.averageUserRating > 0) {
                String.format("%.1f", stats.averageUserRating)
            } else {
                "—"
            }

            // Update unified category cards
            val totalWatched = stats.totalWatchedMovies + stats.totalWatchedTvShows
            val totalPlanned = stats.totalPlannedMovies + stats.totalPlannedTvShows
            val totalWatching = stats.totalWatchingMovies + stats.totalWatchingTvShows

            tvWatchedCount.text = totalWatched.toString()
            tvPlannedCount.text = totalPlanned.toString()
            tvWatchingCount.text = totalWatching.toString()

            // Animate counters only when the value actually changed —
            // otherwise every stats emission makes them bounce/flash
            if (totalWatched != lastWatchedCount) {
                lastWatchedCount = totalWatched
                animateCounterUpdate(tvWatchedCount)
            }
            if (totalPlanned != lastPlannedCount) {
                lastPlannedCount = totalPlanned
                animateCounterUpdate(tvPlannedCount)
            }
            if (totalWatching != lastWatchingCount) {
                lastWatchingCount = totalWatching
                animateCounterUpdate(tvWatchingCount)
            }
        }
    }

    private fun animateCounterUpdate(textView: View) {
        // Scale bounce + color flash effect
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.3f, 0.9f, 1.1f, 1f),
                ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.3f, 0.9f, 1.1f, 1f)
            )
            duration = 600
            interpolator = OvershootInterpolator(1.5f)
        }
        
        // Subtle alpha flash
        val alphaFlash = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0.6f, 1f).apply {
            duration = 300
        }
        
        AnimatorSet().apply {
            playTogether(scaleUp, alphaFlash)
            start()
        }
    }

    private fun animateEntranceElements() {
        // Initial state - hide elements
        val elementsToAnimate = listOf(
            binding.heroSpotlightCard,
            binding.cardWatched,
            binding.cardPlanned,
            binding.cardWatching,
            binding.btnTrending,
            binding.btnUpcomingReleases,
            binding.btnCollections,
            binding.btnFriends,
            binding.btnSearchMovies
        )

        elementsToAnimate.forEach { view ->
            view.alpha = 0f
            view.scaleX = 0.7f
            view.scaleY = 0.7f
            view.translationY = 60f
        }

        // Animate FAB
        binding.fabAdd.alpha = 0f
        binding.fabAdd.scaleX = 0f
        binding.fabAdd.scaleY = 0f
        binding.fabAdd.rotation = -45f

        // Animate floating orbs with subtle pulsing
        listOfNotNull(binding.floatingOrb1, binding.floatingOrb2, binding.floatingOrb3).forEachIndexed { index, orb ->
            orb.alpha = 0f
            orb.scaleX = 0.5f
            orb.scaleY = 0.5f
            
            viewLifecycleOwner.lifecycleScope.launch {
                delay(200L + index * 150L)
                ObjectAnimator.ofFloat(orb, "alpha", 0f, if (index == 0) 0.5f else 0.35f).apply {
                    duration = 800
                    start()
                }
                ObjectAnimator.ofFloat(orb, "scaleX", 0.5f, 1f).apply {
                    duration = 800
                    interpolator = OvershootInterpolator(1.5f)
                    start()
                }
                ObjectAnimator.ofFloat(orb, "scaleY", 0.5f, 1f).apply {
                    duration = 800
                    interpolator = OvershootInterpolator(1.5f)
                    start()
                }
                
                // Add continuous gentle pulsing after entrance
                delay(800L)
                val pulseX = ObjectAnimator.ofFloat(orb, "scaleX", 1f, 1.08f, 1f).apply {
                    duration = 3000L + index * 500L
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                val pulseY = ObjectAnimator.ofFloat(orb, "scaleY", 1f, 1.08f, 1f).apply {
                    duration = 3000L + index * 500L
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                AnimatorSet().apply {
                    playTogether(pulseX, pulseY)
                    start()
                }
            }
        }

        // Staggered animation for cards with spring effect
        elementsToAnimate.forEachIndexed { index, view ->
            viewLifecycleOwner.lifecycleScope.launch {
                delay(150L + index * 70L)
                
                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.05f, 1f)
                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.05f, 1f)
                val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                val translateY = ObjectAnimator.ofFloat(view, "translationY", 60f, -5f, 0f)
                
                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha, translateY)
                    duration = 500
                    interpolator = OvershootInterpolator(1.5f)
                    start()
                }
            }
        }

        // FAB bounce and rotate animation
        viewLifecycleOwner.lifecycleScope.launch {
            delay(700L)
            
            val scaleX = ObjectAnimator.ofFloat(binding.fabAdd, "scaleX", 0f, 1.3f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.fabAdd, "scaleY", 0f, 1.3f, 1f)
            val alpha = ObjectAnimator.ofFloat(binding.fabAdd, "alpha", 0f, 1f)
            val rotation = ObjectAnimator.ofFloat(binding.fabAdd, "rotation", -45f, 0f)
            
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha, rotation)
                duration = 600
                interpolator = OvershootInterpolator(2.5f)
                start()
            }
        }
        
        // Animate header with a subtle slide-down
        binding.headerContainer?.let { header ->
            header.alpha = 0f
            header.translationY = -30f
            header.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupCardPressEffects() {
        val cardsWithPressEffect = listOf(
            binding.cardWatched,
            binding.cardPlanned,
            binding.cardWatching,
            binding.btnSearchMovies,
            binding.btnTrending,
            binding.btnUpcomingReleases,
            binding.btnCollections,
            binding.btnFriends
        )

        cardsWithPressEffect.forEach { card ->
            // Remember the resting elevation from XML (all these cards are flat, 0dp)
            card.setTag(R.id.tag_rest_elevation, (card as? com.google.android.material.card.MaterialCardView)?.cardElevation ?: 0f)
            card.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        animatePress(v, true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        animatePress(v, false)
                    }
                }
                false // Don't consume the event - let click listener handle it
            }
        }
    }

    private fun animatePress(view: View, isPressed: Boolean) {
        val scale = if (isPressed) 0.95f else 1f
        // Return to the card's own resting elevation instead of a hardcoded value,
        // otherwise flat cards keep a stuck 8dp shadow after the first tap.
        val restingElevation = (view.getTag(R.id.tag_rest_elevation) as? Float) ?: 0f
        val elevation = if (isPressed) 2f else restingElevation
        
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
            
        // For MaterialCardViews, also animate elevation
        if (view is com.google.android.material.card.MaterialCardView) {
            ObjectAnimator.ofFloat(view, "cardElevation", view.cardElevation, elevation).apply {
                duration = 100
                start()
            }
        }
    }

    private fun formatTotalTime(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> getString(R.string.time_format_minutes, minutes)
            minutes == 0 -> getString(R.string.time_format_hours, hours)
            else -> getString(R.string.time_format_hours_minutes, hours, minutes)
        }
    }

    private fun navigateToWatchedList(isMovie: Boolean) {
        // For now, just navigate to the existing watched fragment
        findNavController().navigate(R.id.watchedFragment)
    }

    private fun navigateToPlannedList(isMovie: Boolean) {
        findNavController().navigate(R.id.plannedFragment)
    }

    private fun showQuickAddDialog() {
        // TODO: Implement quick add dialog with search functionality
        val intent = Intent(requireActivity(), EnhancedSearchActivity::class.java).apply {
            putExtra("quickAdd", true)
        }
        startActivity(intent)
    }

    private fun loadData() {
        viewModel.loadStatistics()
        viewModel.loadRecommendations()
        viewModel.loadTrendingForBackground()
        viewModel.loadContinueWatching()
        loadNextEpisodeWidget()
    }

    private fun loadNextEpisodeWidget() {
        viewLifecycleOwner.lifecycleScope.launch {
            val nextEpisode = runCatching {
                withContext(Dispatchers.IO) { findNextEpisodeToWatch() }
            }.getOrNull()

            val currentBinding = _binding ?: return@launch
            if (nextEpisode == null) {
                currentBinding.cardNextEpisodeWidget.visibility = View.GONE
            } else {
                bindNextEpisode(nextEpisode)
            }
        }
    }

    private suspend fun findNextEpisodeToWatch(): NextEpisodeToWatch? {
        val watchingShows = appRepository.getWatchingItemsRawSync()
            .filter { it.mediaType == "tv" }

        for (show in watchingShows) {
            val episodes = tvShowProgressDao.getProgressForShowSync(show.id)
                .filter { it.seasonNumber > 0 }

            if (episodes.isEmpty()) {
                return NextEpisodeToWatch(
                    show = show,
                    seasonNumber = show.currentSeason?.coerceAtLeast(1) ?: 1,
                    episodeNumber = show.currentEpisode?.coerceAtLeast(1) ?: 1,
                    episodeName = null,
                    watchedEpisodes = 0,
                    totalEpisodes = 0,
                    hasDetailedProgress = false
                )
            }

            val next = episodes.asSequence()
                .filterNot { it.watched }
                .minWithOrNull(compareBy<TvShowProgress> { it.seasonNumber }.thenBy { it.episodeNumber })
                ?: continue

            return NextEpisodeToWatch(
                show = show,
                seasonNumber = next.seasonNumber,
                episodeNumber = next.episodeNumber,
                episodeName = next.episodeName,
                watchedEpisodes = episodes.count { it.watched },
                totalEpisodes = episodes.size,
                hasDetailedProgress = true
            )
        }

        return null
    }

    private fun bindNextEpisode(nextEpisode: NextEpisodeToWatch) = with(binding) {
        cardNextEpisodeWidget.visibility = View.VISIBLE
        tvNextEpisodeShowTitle.text = nextEpisode.show.title

        val episodeCode = getString(
            R.string.season_episode_format,
            nextEpisode.seasonNumber,
            nextEpisode.episodeNumber
        )
        tvNextEpisodeCode.text = nextEpisode.episodeName
            ?.takeIf { it.isNotBlank() }
            ?.let { "$episodeCode • $it" }
            ?: episodeCode
        tvNextEpisodeProgress.text = if (nextEpisode.totalEpisodes > 0) {
            getString(
                R.string.episodes_progress,
                nextEpisode.watchedEpisodes,
                nextEpisode.totalEpisodes
            )
        } else {
            getString(R.string.no_data)
        }

        ivNextEpisodePoster.load(nextEpisode.show.posterPath?.let {
            "https://image.tmdb.org/t/p/w185$it"
        }) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder)
            error(R.drawable.ic_placeholder)
        }

        cardNextEpisodeWidget.setOnClickListener {
            startActivity(Intent(requireContext(), TvDetailsActivity::class.java).apply {
                putExtra("ITEM_ID", nextEpisode.show.id)
                putExtra("MEDIA_TYPE", "tv")
            })
        }

        btnMarkNextEpisodeWatched.isEnabled = true
        btnMarkNextEpisodeWatched.setOnClickListener { actionView ->
            actionView.isEnabled = false
            actionView.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).withEndAction {
                actionView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            viewLifecycleOwner.lifecycleScope.launch {
                val saved = runCatching {
                    withContext(Dispatchers.IO) { markNextEpisodeWatched(nextEpisode) }
                }.isSuccess

                if (_binding == null) return@launch
                if (saved) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        R.string.episode_marked_toast,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadNextEpisodeWidget()
                    viewModel.loadContinueWatching()
                } else {
                    actionView.isEnabled = true
                }
            }
        }
    }

    private suspend fun markNextEpisodeWatched(nextEpisode: NextEpisodeToWatch) {
        val now = System.currentTimeMillis()
        var episodes: List<TvShowProgress>

        if (!nextEpisode.hasDetailedProgress) {
            episodes = seedEpisodeProgress(nextEpisode, now) ?: run {
                appRepository.updateWatchingEpisodePosition(
                    id = nextEpisode.show.id,
                    mediaType = nextEpisode.show.mediaType,
                    seasonNumber = nextEpisode.seasonNumber,
                    episodeNumber = nextEpisode.episodeNumber + 1
                )
                return
            }
        } else {
            tvShowProgressDao.setEpisodeWatched(
                tvShowId = nextEpisode.show.id,
                seasonNumber = nextEpisode.seasonNumber,
                episodeNumber = nextEpisode.episodeNumber,
                watched = true,
                watchedAt = now
            )
            episodes = tvShowProgressDao.getProgressForShowSync(nextEpisode.show.id)
                .filter { it.seasonNumber > 0 }
        }

        val watchedEpisodes = episodes.filter { it.watched }
        val averageEpisodeRuntime = episodes
            .mapNotNull { it.episodeRuntime?.takeIf { runtime -> runtime > 0 } }
            .average()
            .takeIf { !it.isNaN() }
            ?.toInt()
        val existingItem = appRepository.getWatchedItemById(nextEpisode.show.id, "tv")
        val updatedItem = existingItem?.copy(
            runtime = watchedEpisodes.sumOf { it.episodeRuntime ?: 0 },
            totalEpisodes = episodes.size,
            episodeRuntime = averageEpisodeRuntime,
            lastUpdated = now
        ) ?: WatchedItem(
            id = nextEpisode.show.id,
            title = nextEpisode.show.title,
            posterPath = nextEpisode.show.posterPath,
            releaseDate = nextEpisode.show.releaseDate,
            runtime = watchedEpisodes.sumOf { it.episodeRuntime ?: 0 },
            mediaType = "tv",
            episodeRuntime = averageEpisodeRuntime,
            totalEpisodes = episodes.size,
            lastUpdated = now
        )
        appRepository.addWatchedItem(updatedItem)
    }

    private suspend fun seedEpisodeProgress(
        nextEpisode: NextEpisodeToWatch,
        watchedAt: Long
    ): List<TvShowProgress>? {
        val showDetails = appRepository.getTvShowDetails(nextEpisode.show.id)
        val seasonCount = maxOf(
            showDetails.numberOfSeasons ?: 0,
            showDetails.seasons?.mapNotNull { it.seasonNumber }?.filter { it > 0 }?.maxOrNull() ?: 0
        )
        if (seasonCount <= 0) return null

        val today = LocalDate.now().toString()
        val rows = appRepository.getAllSeasonsDetails(nextEpisode.show.id, seasonCount)
            .flatMap { season ->
                val defaultSeasonNumber = season.seasonNumber ?: 0
                season.episodes.orEmpty().mapNotNull { episode ->
                    val seasonNumber = episode.seasonNumber ?: defaultSeasonNumber
                    val episodeNumber = episode.episodeNumber ?: 0
                    val airDate = episode.airDate
                    if (seasonNumber <= 0 || episodeNumber <= 0 ||
                        (airDate != null && airDate.isNotBlank() && airDate > today)
                    ) return@mapNotNull null

                    val watched = seasonNumber < nextEpisode.seasonNumber ||
                        (seasonNumber == nextEpisode.seasonNumber && episodeNumber <= nextEpisode.episodeNumber)
                    TvShowProgress(
                        tvShowId = nextEpisode.show.id,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeName = episode.name,
                        episodeRuntime = episode.runtime,
                        watched = watched,
                        watchedAt = if (
                            seasonNumber == nextEpisode.seasonNumber &&
                            episodeNumber == nextEpisode.episodeNumber
                        ) watchedAt else null
                    )
                }
            }
            .distinctBy { Triple(it.seasonNumber, it.episodeNumber, it.tvShowId) }
            .sortedWith(compareBy<TvShowProgress> { it.seasonNumber }.thenBy { it.episodeNumber })

        if (rows.isEmpty()) return null
        tvShowProgressDao.insertEpisodes(rows)
        return rows
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadData()
    }

    private fun setupContinueWatching() {
        continueWatchingAdapter = com.example.movietime.ui.today.adapters.ContinueWatchingAdapter { item ->
            val intent = if (item.mediaType == "tv") {
                Intent(requireContext(), TvDetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "tv")
                }
            } else {
                Intent(requireContext(), DetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "movie")
                }
            }
            startActivity(intent)
        }

        binding.rvContinueWatching.apply {
            adapter = continueWatchingAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            setItemViewCacheSize(10)
        }
    }

    private fun setupRecentActivity() {
        recentActivityAdapter = RecentActivityAdapter { item ->
            // Use mediaType from item to decide destination and params
            val intent = if (item.mediaType == "tv") {
                Intent(requireContext(), TvDetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "tv")
                }
            } else {
                Intent(requireContext(), DetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", "movie")
                }
            }
            startActivity(intent)
        }
        
        binding.rvRecentActivity.apply {
            adapter = recentActivityAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_cascade)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
