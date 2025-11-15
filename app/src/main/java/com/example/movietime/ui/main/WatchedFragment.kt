package com.example.movietime.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.movietime.R
import com.example.movietime.databinding.FragmentWatchedBinding
import com.example.movietime.ui.search.SearchActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchedFragment : Fragment() {

    private var _binding: FragmentWatchedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        loadData()
    }

    private fun setupClickListeners() {
        // Category cards
        binding.cardWatchedMovies.setOnClickListener {
            // Navigate to watched movies list
            navigateToWatchedList(isMovie = true)
        }

        binding.cardWatchedTvShows.setOnClickListener {
            // Navigate to watched TV shows list
            navigateToWatchedList(isMovie = false)
        }

        binding.cardPlannedMovies.setOnClickListener {
            // TODO: Navigate to planned movies list when implemented
        }

        binding.cardPlannedTvShows.setOnClickListener {
            // TODO: Navigate to planned TV shows list when implemented
        }

        // Quick action buttons
        binding.btnSearchMovies.setOnClickListener {
            val intent = Intent(requireActivity(), SearchActivity::class.java)
            startActivity(intent)
        }

        binding.btnTrending.setOnClickListener {
            findNavController().navigate(R.id.trendingFragment)
        }
    }

    private fun navigateToWatchedList(isMovie: Boolean) {
        // TODO: Create separate fragments or activities for filtered lists
        // For now, show a placeholder
        val type = if (isMovie) "фільми" else "серіали"
        val message = "Перегляд переглянутих: $type"
        // Could navigate to a filtered version of the list
    }



    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe statistics
            viewModel.getWatchedStatistics().collect { stats ->
                updateStatistics(stats)
            }
        }
    }

    private fun updateStatistics(stats: WatchedStatistics) {
        with(binding) {
            // Update total time
            tvTotalTime.text = formatTotalTime(stats.totalMinutes)

            // Update counts
            tvWatchedMoviesCount.text = stats.movieCount.toString()
            tvWatchedTvShowsCount.text = stats.tvShowCount.toString()

            // Update planned counts (if available)
            tvPlannedMoviesCount.text = stats.plannedMovieCount.toString()
            tvPlannedTvShowsCount.text = stats.plannedTvShowCount.toString()
        }
    }

    private fun formatTotalTime(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> "${minutes} хв"
            minutes == 0 -> "${hours} год"
            else -> "${hours} год ${minutes} хв"
        }
    }

    private fun loadData() {
        viewModel.loadStatistics()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}