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
        binding.watchedRecyclerView.apply {
            adapter = watchedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.watchedList.observe(viewLifecycleOwner) { watchedItems ->
            watchedAdapter.submitList(watchedItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}