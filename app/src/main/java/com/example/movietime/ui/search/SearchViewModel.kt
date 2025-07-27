package com.example.movietime.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.ApiMovie
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _searchResult = MutableLiveData<List<ApiMovie>>()
    val searchResult: LiveData<List<ApiMovie>> = _searchResult

    fun searchMovies(query: String) {
        viewModelScope.launch {
            try {
                val response = repository.searchMovies(query)
                _searchResult.value = response.results
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching movies", e)
                _searchResult.value = emptyList()
            }
        }
    }
}