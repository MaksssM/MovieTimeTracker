package com.example.movietime.ui.trending

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _trendingMovies = MutableLiveData<List<Any>>()
    val trendingMovies: LiveData<List<Any>> = _trendingMovies

    init {
        loadTrendingContent()
    }

    private fun loadTrendingContent() {
        viewModelScope.launch {
            try {
                val moviesResponse = repository.getPopularMovies()
                val tvResponse = repository.getPopularTvShows()

                // Об'єднуємо фільми та серіали в один список
                val allContent = mutableListOf<Any>()
                allContent.addAll(moviesResponse.results)
                allContent.addAll(tvResponse.results)

                Log.d(
                    "TrendingViewModel",
                    "Loaded ${moviesResponse.results.size} movies and ${tvResponse.results.size} TV shows"
                )
                _trendingMovies.value = allContent
            } catch (e: Exception) {
                Log.e("TrendingViewModel", "Error loading trending content", e)
                _trendingMovies.value = emptyList()
            }
        }
    }
}