package com.example.movietime.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.ActivitySearchBinding
import com.example.movietime.ui.ViewModelFactory
import com.example.movietime.ui.details.DetailsActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var viewModel: SearchViewModel
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = ViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupSearchInput()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val searchAdapter = SearchAdapter { mediaItem ->
            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtra(DetailsActivity.EXTRA_ID, mediaItem.id)
                putExtra(DetailsActivity.EXTRA_TYPE, mediaItem.mediaType)
                putExtra(DetailsActivity.EXTRA_TITLE, mediaItem.universalTitle)
                putExtra(DetailsActivity.EXTRA_POSTER_PATH, mediaItem.posterPath)
                putExtra(DetailsActivity.EXTRA_OVERVIEW, mediaItem.overview)
            }
            startActivity(intent)
        }

        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun setupSearchInput() {
        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = MainScope().launch {
                    delay(500L) // Задержка в полсекунды
                    s?.let {
                        if (it.toString().isNotBlank()) {
                            viewModel.search(it.toString())
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
            (binding.rvSearchResults.adapter as SearchAdapter).submitList(results)
        }
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }
}