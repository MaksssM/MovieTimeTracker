package com.example.movietime.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.ActivitySearchBinding
import com.example.movietime.ui.details.DetailsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter()

        searchAdapter.onItemClick = { movie ->
            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtra("MOVIE_ID", movie.id)
            }
            startActivity(intent)
        }

        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.searchResult.observe(this) { movies ->
            searchAdapter.submitList(movies)
        }
    }

    private fun setupSearch() {
        binding.etSearchQuery.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString()
                if (query.isNotBlank()) {
                    viewModel.searchMovies(query)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
}