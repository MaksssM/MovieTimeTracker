package com.example.movietime.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.FragmentWatchedBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.movietime.ui.search.SearchActivity
import com.example.movietime.R
import com.example.movietime.BuildConfig
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class WatchedFragment : Fragment() {

    private var _binding: FragmentWatchedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var watchedAdapter: WatchedAdapter

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
        observeViewModel()

        binding.fabSearch.setOnClickListener {
            val intent = Intent(requireActivity(), SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        watchedAdapter = WatchedAdapter()
        watchedAdapter.onItemClick = { item ->
            // optional: open details
        }
        watchedAdapter.onDeleteClick = { item ->
            performDeleteWithUndo(item)
        }

        binding.rvWatched.apply {
            adapter = watchedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun performDeleteWithUndo(item: com.example.movietime.data.db.WatchedItem) {
        // remove immediately
        viewModel.deleteWatchedItem(item)
        // show snackbar with undo
        Snackbar.make(binding.root, getString(R.string.undo_message), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo)) {
                // re-add item
                viewModel.addWatchedItem(item)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.watchedList.observe(viewLifecycleOwner) { watchedItems ->
            watchedAdapter.submitList(watchedItems)
            // Update count in header
            val count = watchedItems.size
            binding.tvTotalCount.text = count.toString()

            // Show/hide empty state
            if (watchedItems.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvWatched.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvWatched.visibility = View.VISIBLE
            }
        }

        // Observe total time formatted
        viewModel.totalTimeFormatted.observe(viewLifecycleOwner) { formatted ->
            binding.tvTotalTime.text = formatted
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}