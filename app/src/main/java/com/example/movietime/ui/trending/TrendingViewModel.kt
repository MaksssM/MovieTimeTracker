package com.example.movietime.ui.trending

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
class TrendingViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _trendingMovies = MutableLiveData<List<ApiMovie>>()
    val trendingMovies: LiveData<List<ApiMovie>> = _trendingMovies

    init {
        loadTrendingMovies()
    }

    private fun loadTrendingMovies() {
        viewModelScope.launch {
            try {
                val response = repository.getPopularMovies()
                _trendingMovies.value = response.results
            } catch (e: Exception) {
                // ДОДАЙТЕ ЦІ РЯДКИ
                Log.e("TrendingViewModel", "Error loading trending movies", e)
                _trendingMovies.value = emptyList()
            }
        }
    }
}