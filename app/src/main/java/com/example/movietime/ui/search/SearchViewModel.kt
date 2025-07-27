package com.example.movietime.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.ApiMediaItem
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: AppRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<ApiMediaItem>>()
    val searchResults: LiveData<List<ApiMediaItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun search(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val results = repository.search(query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }
}