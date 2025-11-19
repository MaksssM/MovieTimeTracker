package com.example.movietime.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.FragmentWatchedBinding
import com.example.movietime.ui.adapters.ContentAdapter
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchedFragment : Fragment() {

    private var _binding: FragmentWatchedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var contentAdapter: ContentAdapter

    private var currentFilter = FilterType.ALL

    enum class FilterType {
        ALL, MOVIES, TV_SHOWS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerView() {
        contentAdapter = ContentAdapter(
            onItemClick = { item ->
                val intent = if (item.mediaType == "movie") {
                    Intent(requireContext(), DetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.id)
                        putExtra("MEDIA_TYPE", "movie")
                    }
                } else {
                    Intent(requireContext(), TvDetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.id)
                        putExtra("MEDIA_TYPE", "tv")
                    }
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                viewModel.deleteWatchedItem(item)
            }
        )

        binding.rvWatchedItems.apply {
            adapter = contentAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.all))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.movies))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tv_shows))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> FilterType.ALL
                    1 -> FilterType.MOVIES
                    2 -> FilterType.TV_SHOWS
                    else -> FilterType.ALL
                }
                updateFilteredList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        // Observe total watch time
        viewModel.totalMinutes.observe(viewLifecycleOwner) { totalMinutes ->
            binding.tvTotalTime.text = formatTotalTime(totalMinutes)
        }

        // Observe watched list
        viewModel.watchedList.observe(viewLifecycleOwner) { watchedItems ->
            binding.tvTotalCount.text = watchedItems.size.toString()
            updateFilteredList()
        }
    }

    private fun updateFilteredList() {
        val allItems = viewModel.watchedList.value ?: emptyList()

        val filteredItems = when (currentFilter) {
            FilterType.ALL -> allItems
            FilterType.MOVIES -> allItems.filter { it.mediaType == "movie" }
            FilterType.TV_SHOWS -> allItems.filter { it.mediaType == "tv" }
        }

        contentAdapter.updateItems(filteredItems)
        binding.layoutEmpty.isVisible = filteredItems.isEmpty()
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

    private fun loadData() {
        // Data is automatically loaded through Flow observers
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
