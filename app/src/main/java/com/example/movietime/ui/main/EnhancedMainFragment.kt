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
import com.example.movietime.data.model.DetailedStatistics
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.upcoming.UpcomingReleasesActivity
import com.example.movietime.ui.friends.FriendsActivity
import com.example.movietime.ui.planned.PlannedActivity
import com.example.movietime.ui.watching.WatchingActivity
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.data.model.RecentActivityItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EnhancedMainFragment : Fragment() {

    private var _binding: FragmentEnhancedMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EnhancedMainViewModel by viewModels()
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private lateinit var recommendationsAdapter: com.example.movietime.ui.adapters.RecommendationsAdapter

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
        setupParallaxEffect()
        observeViewModel()
        loadData()
        animateEntranceElements()
    }

    private var lastClickTime = 0L
    private val clickDebounceTime = 500L // 500ms debounce

    private fun setupClickListeners() {
        // Category cards - unified
        binding.cardWatched.setOnClickListener {
            handleClickWithDebounce {
                findNavController().navigate(R.id.watchedFragment)
            }
        }

        binding.cardPlanned.setOnClickListener {
            handleClickWithDebounce {
                startActivity(Intent(requireActivity(), PlannedActivity::class.java))
            }
        }

        binding.cardWatching.setOnClickListener {
            handleClickWithDebounce {
                startActivity(Intent(requireActivity(), WatchingActivity::class.java))
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
                startActivity(Intent(requireActivity(), FriendsActivity::class.java))
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
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
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

    private fun updateStatistics(stats: DetailedStatistics) {
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

            // Animate counter updates
            animateCounterUpdate(tvWatchedCount, totalWatched)
            animateCounterUpdate(tvPlannedCount, totalPlanned)
            animateCounterUpdate(tvWatchingCount, totalWatching)
        }
    }

    private fun animateCounterUpdate(textView: View, @Suppress("UNUSED_PARAMETER") newValue: Int) {
        ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateEntranceElements() {
        // Initial state - hide elements
        val elementsToAnimate = listOf(
            binding.cardWatched,
            binding.cardPlanned,
            binding.cardWatching,
            binding.btnSearchMovies,
            binding.btnTrending,
            binding.btnUpcomingReleases,
            binding.btnCollections,
            binding.btnFriends
        )

        elementsToAnimate.forEach { view ->
            view.alpha = 0f
            view.scaleX = 0.8f
            view.scaleY = 0.8f
            view.translationY = 50f
        }

        // Animate FAB
        binding.fabAdd.alpha = 0f
        binding.fabAdd.scaleX = 0f
        binding.fabAdd.scaleY = 0f

        // Staggered animation for cards
        elementsToAnimate.forEachIndexed { index, view ->
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100L + index * 60L) // Slightly faster stagger since more items
                
                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)
                val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                val translateY = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                
                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha, translateY)
                    duration = 400
                    interpolator = OvershootInterpolator(1.2f)
                    start()
                }
            }
        }

        // FAB bounce animation
        viewLifecycleOwner.lifecycleScope.launch {
            delay(800L) // Delayed a bit more
            
            val scaleX = ObjectAnimator.ofFloat(binding.fabAdd, "scaleX", 0f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.fabAdd, "scaleY", 0f, 1.2f, 1f)
            val alpha = ObjectAnimator.ofFloat(binding.fabAdd, "alpha", 0f, 1f)
            
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 500
                interpolator = OvershootInterpolator(2f)
                start()
            }
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
        val elevation = if (isPressed) 2f else 8f
        
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
        val intent = Intent(requireActivity(), PlannedActivity::class.java).apply {
            putExtra("isMovie", isMovie)
        }
        startActivity(intent)
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
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadData()
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
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_cascade)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
