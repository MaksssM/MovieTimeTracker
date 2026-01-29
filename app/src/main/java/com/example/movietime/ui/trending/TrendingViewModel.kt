package com.example.movietime.ui.trending

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _trendingMovies = MutableLiveData<List<Any>>()
    val trendingMovies: LiveData<List<Any>> = _trendingMovies

    // Discovery Optimization
    private var seenItemIds = setOf<String>()
    
    init {
        refreshSeenItems()
        loadTrendingContent()
    }

    private fun refreshSeenItems() {
        viewModelScope.launch {
            try {
                val ids = withContext(Dispatchers.IO) {
                    repository.getAllSeenItemIds()
                }
                seenItemIds = ids.map { "${it.mediaType}:${it.id}" }.toSet()
            } catch (e: Exception) {
                Log.e("TrendingViewModel", "Error refreshing seen items", e)
            }
        }
    }

    private fun calculateDiscoveryScore(item: Any): Double {
        val (id, mediaType, popularity) = when (item) {
            is MovieResult -> Triple(item.id, "movie", item.popularity.toDouble())
            is TvShowResult -> Triple(item.id, "tv", item.popularity.toDouble())
            else -> Triple(0, "unknown", 0.0)
        }

        val isSeen = seenItemIds.contains("$mediaType:$id")
        val penaltyFactor = if (isSeen) 0.3 else 1.0 // 70% penalty for seen items
        
        return popularity * penaltyFactor
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
                
                // Discovery Optimization: Sort by popularity with a penalty for seen items
                val sortedContent = allContent.sortedByDescending { item ->
                    calculateDiscoveryScore(item)
                }
                
                _trendingMovies.value = sortedContent
            } catch (e: Exception) {
                Log.e("TrendingViewModel", "Error loading trending content", e)
                _trendingMovies.value = emptyList()
            }
        }
    }
}