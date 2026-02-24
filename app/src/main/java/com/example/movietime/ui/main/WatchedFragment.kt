package com.example.movietime.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.FragmentWatchedBinding
import com.example.movietime.ui.adapters.ContentAdapter
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchedFragment : Fragment() {

    private var _binding: FragmentWatchedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var contentAdapter: ContentAdapter

    private var currentFilter = FilterType.ALL
    private var currentSort = SortType.DATE_NEWEST
    private var searchQuery = ""

    enum class FilterType {
        ALL, MOVIES, TV_SHOWS
    }

    enum class SortType {
        DATE_NEWEST, DATE_OLDEST,
        NAME_ASC, NAME_DESC,
        RATING_HIGH, RATING_LOW,
        RUNTIME_LONG, RUNTIME_SHORT
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
        setupSortAndSearch()
        observeViewModel()
        loadData()
        animateEntrance()
    }

    private fun animateEntrance() {
        // Animate header stats with scale bounce
        // Animate search/sort bar sliding in
        binding.etSearch?.let { searchField ->
            searchField.alpha = 0f
            searchField.translationY = 30f
            searchField.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        
        binding.btnSort?.let { sortBtn ->
            sortBtn.alpha = 0f
            sortBtn.scaleX = 0f
            sortBtn.scaleY = 0f
            sortBtn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(350)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }

        // Animate tab layout
        binding.tabLayout?.let { tabLayout ->
            tabLayout.alpha = 0f
            tabLayout.translationX = -50f
            tabLayout.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
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
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_confirmation_title)
                    .setMessage(getString(R.string.delete_watched_confirmation, item.title))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteWatchedItem(item)
                        Toast.makeText(requireContext(), R.string.item_deleted, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            },
            onRewatchClick = { item ->
                viewModel.incrementWatchCount(item)
            }
        )

        binding.rvWatchedItems.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            setItemViewCacheSize(15)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.layout_animation_slide_up
            )
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

    private fun setupSortAndSearch() {
        // Search text listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                updateFilteredList()
            }
        })

        // Sort button
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
            getString(R.string.sort_by_rating_low),
            getString(R.string.sort_by_runtime_long),
            getString(R.string.sort_by_runtime_short)
        )

        val currentIndex = when (currentSort) {
            SortType.DATE_NEWEST -> 0
            SortType.DATE_OLDEST -> 1
            SortType.NAME_ASC -> 2
            SortType.NAME_DESC -> 3
            SortType.RATING_HIGH -> 4
            SortType.RATING_LOW -> 5
            SortType.RUNTIME_LONG -> 6
            SortType.RUNTIME_SHORT -> 7
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                currentSort = when (which) {
                    0 -> SortType.DATE_NEWEST
                    1 -> SortType.DATE_OLDEST
                    2 -> SortType.NAME_ASC
                    3 -> SortType.NAME_DESC
                    4 -> SortType.RATING_HIGH
                    5 -> SortType.RATING_LOW
                    6 -> SortType.RUNTIME_LONG
                    7 -> SortType.RUNTIME_SHORT
                    else -> SortType.DATE_NEWEST
                }
                updateFilteredList()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.totalMinutes.observe(viewLifecycleOwner) { totalMinutes ->
            binding.tvTotalTime.text = formatTotalTime(totalMinutes)
        }

        viewModel.watchedList.observe(viewLifecycleOwner) { watchedItems ->
            binding.tvTotalCount.text = watchedItems.size.toString()
            updateFilteredList()
        }
    }

    private fun updateFilteredList() {
        val allItems = viewModel.watchedList.value ?: emptyList()

        var filteredItems = when (currentFilter) {
            FilterType.ALL -> allItems
            FilterType.MOVIES -> allItems.filter { it.mediaType == "movie" }
            FilterType.TV_SHOWS -> allItems.filter { it.mediaType == "tv" }
        }

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filteredItems = filteredItems.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply sort
        filteredItems = applySorting(filteredItems)

        contentAdapter.updateItems(filteredItems)
        binding.layoutEmpty.isVisible = filteredItems.isEmpty()
        binding.rvWatchedItems.scheduleLayoutAnimation()
    }

    private fun applySorting(items: List<WatchedItem>): List<WatchedItem> {
        return when (currentSort) {
            SortType.DATE_NEWEST -> items.sortedByDescending { it.lastUpdated ?: 0L }
            SortType.DATE_OLDEST -> items.sortedBy { it.lastUpdated ?: 0L }
            SortType.NAME_ASC -> items.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
            SortType.RATING_HIGH -> items.sortedByDescending { it.voteAverage ?: 0.0 }
            SortType.RATING_LOW -> items.sortedBy { it.voteAverage ?: 0.0 }
            SortType.RUNTIME_LONG -> items.sortedByDescending { it.runtime ?: 0 }
            SortType.RUNTIME_SHORT -> items.sortedBy { it.runtime ?: 0 }
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
