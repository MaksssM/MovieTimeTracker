package com.example.movietime.ui.main

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.movietime.R
import com.example.movietime.databinding.FragmentEnhancedMainBinding
import com.example.movietime.data.model.DetailedStatistics
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.example.movietime.ui.upcoming.UpcomingReleasesActivity
import com.example.movietime.ui.friends.FriendsActivity
import com.example.movietime.ui.planned.PlannedActivity
import com.example.movietime.ui.adapters.ActivityAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EnhancedMainFragment : Fragment() {

    private var _binding: FragmentEnhancedMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EnhancedMainViewModel by viewModels()
    private lateinit var activityAdapter: ActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnhancedMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadData()
        animateEntranceElements()
    }

    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter { activity ->
            // Handle activity item click - navigate to details
            // TODO: Implement navigation to movie/TV show details
        }

        binding.rvRecentActivity.apply {
            adapter = activityAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // Category cards
        binding.cardWatchedMovies.setOnClickListener {
            navigateToWatchedList(isMovie = true)
        }

        binding.cardWatchedTvShows.setOnClickListener {
            navigateToWatchedList(isMovie = false)
        }

        binding.cardPlannedMovies.setOnClickListener {
            navigateToPlannedList(isMovie = true)
        }

        binding.cardPlannedTvShows.setOnClickListener {
            navigateToPlannedList(isMovie = false)
        }

        // Quick action buttons
        binding.btnSearchMovies.setOnClickListener {
            startActivity(Intent(requireActivity(), EnhancedSearchActivity::class.java))
        }

        binding.btnTrending.setOnClickListener {
            findNavController().navigate(R.id.trendingFragment)
        }

        binding.btnUpcomingReleases.setOnClickListener {
            startActivity(Intent(requireActivity(), UpcomingReleasesActivity::class.java))
        }

        binding.btnFriends.setOnClickListener {
            startActivity(Intent(requireActivity(), FriendsActivity::class.java))
        }

        // Floating Action Button
        binding.fabQuickAdd.setOnClickListener {
            showQuickAddDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe statistics
            viewModel.getDetailedStatistics().collect { stats ->
                updateStatistics(stats)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe recent activities
            viewModel.getRecentActivities().collect { activities ->
                activityAdapter.submitList(activities)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Load trending content for header background
            viewModel.loadTrendingForBackground()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe trending background
            viewModel.backgroundImage.collect { imageUrl ->
                imageUrl?.let {
                    binding.ivHeaderBackground.load("https://image.tmdb.org/t/p/w780$it") {
                        crossfade(true)
                    }
                }
            }
        }
    }

    private fun updateStatistics(stats: DetailedStatistics) {
        with(binding) {
            // Update header stats
            tvTotalTime.text = formatTotalTime(stats.totalWatchTimeMinutes)
            tvThisMonthCount.text = stats.thisMonthWatched.toString()

            // Update quick stats
            tvWatchedMoviesCount.text = stats.totalWatchedMovies.toString()
            tvWatchedTvShowsCount.text = stats.totalWatchedTvShows.toString()
            tvAverageRating.text = if (stats.averageUserRating > 0) {
                String.format("%.1f", stats.averageUserRating)
            } else {
                "—"
            }

            // Update category cards
            tvWatchedMoviesCountCard.text = stats.totalWatchedMovies.toString()
            tvWatchedTvShowsCountCard.text = stats.totalWatchedTvShows.toString()
            tvPlannedMoviesCount.text = stats.totalPlannedMovies.toString()
            tvPlannedTvShowsCount.text = stats.totalPlannedTvShows.toString()

            // Animate counter updates
            animateCounterUpdate(tvWatchedMoviesCount, stats.totalWatchedMovies)
            animateCounterUpdate(tvWatchedTvShowsCount, stats.totalWatchedTvShows)
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
        val elementsToAnimate = listOf(
            binding.cardWatchedMovies,
            binding.cardWatchedTvShows,
            binding.cardPlannedMovies,
            binding.cardPlannedTvShows
        )

        elementsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 100f

            viewLifecycleOwner.lifecycleScope.launch {
                delay(index * 100L)
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    duration = 400
                    start()
                }
                ObjectAnimator.ofFloat(view, "translationY", 100f, 0f).apply {
                    duration = 400
                    interpolator = DecelerateInterpolator()
                    start()
                }
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
        viewModel.loadRecentActivities()
        // loadUpcomingReleases will be added later
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
