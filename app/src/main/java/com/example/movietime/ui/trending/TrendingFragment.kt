package com.example.movietime.ui.trending

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.FragmentTrendingBinding
import com.example.movietime.ui.search.SearchAdapter
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrendingViewModel by viewModels()
    private lateinit var trendingAdapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        trendingAdapter = SearchAdapter()

        trendingAdapter.onItemClick = { item ->
            when (item) {
                is MovieResult -> {
                    val intent = Intent(requireActivity(), DetailsActivity::class.java)
                    intent.putExtra("ITEM_ID", item.id)
                    intent.putExtra("MEDIA_TYPE", "movie")
                    startActivity(intent)
                }
                is TvShowResult -> {
                    val intent = Intent(requireActivity(), TvDetailsActivity::class.java)
                    intent.putExtra("ITEM_ID", item.id)
                    intent.putExtra("MEDIA_TYPE", "tv")
                    startActivity(intent)
                }
            }
        }

        binding.trendingRecyclerView.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.trendingMovies.observe(viewLifecycleOwner) { movies ->
            trendingAdapter.submitList(movies)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}