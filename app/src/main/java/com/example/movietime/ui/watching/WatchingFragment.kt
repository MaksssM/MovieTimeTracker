package com.example.movietime.ui.watching

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
import com.example.movietime.databinding.FragmentWatchingBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.ui.search.EnhancedSearchActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchingFragment : Fragment() {

    private var _binding: FragmentWatchingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WatchingViewModel by viewModels()

    private lateinit var watchingAdapter: WatchingAdapter
    private var currentFilter = "all"
    private var currentSort = SortType.DATE_NEWEST
    private var searchQuery = ""

    enum class SortType {
        DATE_NEWEST, DATE_OLDEST,
        NAME_ASC, NAME_DESC,
        RATING_HIGH, RATING_LOW
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupSortAndSearch()
        setupClickListeners()
        observeViewModel()
        loadWatchingContent()
        animateEntrance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun animateEntrance() {
        binding.fabAddWatching.alpha = 0f
        binding.fabAddWatching.scaleX = 0f
        binding.fabAddWatching.scaleY = 0f
        binding.fabAddWatching.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(400)
            .setInterpolator(OvershootInterpolator(2.5f))
            .start()

        binding.etSearch.let { search ->
            search.alpha = 0f
            search.translationY = 30f
            search.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.btnSort.let { sort ->
            sort.alpha = 0f
            sort.scaleX = 0f
            sort.scaleY = 0f
            sort.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }
    }

    private fun setupRecyclerView() {
        watchingAdapter = WatchingAdapter(
            onItemClick = { item ->
                val targetActivity = if (item.mediaType == "tv") {
                    TvDetailsActivity::class.java
                } else {
                    DetailsActivity::class.java
                }
                val intent = Intent(requireContext(), targetActivity).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", item.mediaType)
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmDialog(item)
            },
            onMoveToWatchedClick = { item ->
                viewModel.moveToWatched(item)
                Toast.makeText(requireContext(), getString(R.string.moved_to_watched), Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvWatching.apply {
            adapter = watchingAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(15)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.layout_animation_slide_in_right
            )
        }
    }

    private fun showDeleteConfirmDialog(item: WatchedItem) {
        val options = arrayOf(
            getString(R.string.move_to_planned),
            getString(R.string.delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.moveToPlanned(item)
                        Toast.makeText(requireContext(), getString(R.string.moved_to_planned), Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        viewModel.removeFromWatching(item)
                        Toast.makeText(requireContext(), getString(R.string.item_deleted), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "all"
                    1 -> "tv"
                    2 -> "movie"
                    else -> "all"
                }
                filterContent()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.fabAddWatching.setOnClickListener {
            startActivity(Intent(requireContext(), EnhancedSearchActivity::class.java))
        }

        binding.btnBrowseContent.setOnClickListener {
            startActivity(Intent(requireContext(), EnhancedSearchActivity::class.java))
        }
    }

    private fun setupSortAndSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterContent()
            }
        })

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
            getString(R.string.sort_by_rating_low)
        )

        val currentIndex = when (currentSort) {
            SortType.DATE_NEWEST -> 0
            SortType.DATE_OLDEST -> 1
            SortType.NAME_ASC -> 2
            SortType.NAME_DESC -> 3
            SortType.RATING_HIGH -> 4
            SortType.RATING_LOW -> 5
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
                    else -> SortType.DATE_NEWEST
                }
                filterContent()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applySorting(items: List<WatchedItem>): List<WatchedItem> {
        return when (currentSort) {
            SortType.DATE_NEWEST -> items.sortedByDescending { it.lastUpdated ?: 0L }
            SortType.DATE_OLDEST -> items.sortedBy { it.lastUpdated ?: 0L }
            SortType.NAME_ASC -> items.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
            SortType.RATING_HIGH -> items.sortedByDescending { it.voteAverage ?: 0.0 }
            SortType.RATING_LOW -> items.sortedBy { it.voteAverage ?: 0.0 }
        }
    }

    private fun observeViewModel() {
        viewModel.watchingContent.observe(viewLifecycleOwner) { content ->
            binding.layoutLoading.isVisible = false

            val moviesCount = content.count { it.mediaType == "movie" }
            val tvCount = content.count { it.mediaType == "tv" }
            binding.tvWatchingTotalCount.text = content.size.toString()
            binding.tvWatchingMoviesCount.text = moviesCount.toString()
            binding.tvWatchingTvCount.text = tvCount.toString()

            if (content.isEmpty()) {
                showEmptyState()
            } else {
                showContent()
                filterContent()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.layoutLoading.isVisible = isLoading
            binding.rvWatching.isVisible = !isLoading && (viewModel.watchingContent.value?.isNotEmpty() == true)
            binding.layoutEmpty.isVisible = !isLoading && (viewModel.watchingContent.value?.isEmpty() == true)
        }
    }

    private fun loadWatchingContent() {
        viewModel.loadWatchingContent()
    }

    private fun filterContent() {
        val allContent = viewModel.watchingContent.value ?: emptyList()

        var filteredContent = when (currentFilter) {
            "movie" -> allContent.filter { it.mediaType == "movie" }
            "tv" -> allContent.filter { it.mediaType == "tv" }
            else -> allContent
        }

        if (searchQuery.isNotEmpty()) {
            filteredContent = filteredContent.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredContent = applySorting(filteredContent)
        watchingAdapter.updateItems(filteredContent)

        if (filteredContent.isEmpty()) {
            binding.rvWatching.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvWatching.isVisible = true
            binding.layoutEmpty.isVisible = false
            binding.rvWatching.scheduleLayoutAnimation()
        }
    }

    private fun showEmptyState() {
        binding.rvWatching.isVisible = false
        binding.layoutEmpty.isVisible = true
    }

    private fun showContent() {
        binding.rvWatching.isVisible = true
        binding.layoutEmpty.isVisible = false
    }
}
