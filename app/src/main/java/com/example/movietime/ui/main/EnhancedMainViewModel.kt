package com.example.movietime.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.*
import com.example.movietime.data.repository.SimpleEnhancedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnhancedMainViewModel @Inject constructor(
    private val repository: SimpleEnhancedRepository,
    private val recommendationService: com.example.movietime.service.RecommendationService
) : ViewModel() {

    private val _backgroundImage = MutableStateFlow<String?>(null)
    val backgroundImage: StateFlow<String?> = _backgroundImage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun getDetailedStatistics(): Flow<BasicStatistics> {
        return repository.getDetailedStatistics()
    }

    fun getRecentActivities(): Flow<List<RecentActivityItem>> {
        return repository.getRecentActivities()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Trigger a fresh collection of statistics
                repository.getDetailedStatistics().collect { stats ->
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadRecentActivities() {
        viewModelScope.launch {
            try {
                // Activities are loaded via Flow, no need for explicit loading
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadTrendingForBackground() {
        viewModelScope.launch {
            try {
                val result = repository.getTrendingContent("week")
                result.onSuccess { backdropPath ->
                    _backgroundImage.value = backdropPath
                }.onFailure { error ->
                    _error.value = error.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // Removed loadUpcomingReleases for now - will implement later

    private val _recommendations = MutableStateFlow<List<Any>>(emptyList())
    val recommendations: StateFlow<List<Any>> = _recommendations.asStateFlow()

    fun loadRecommendations() {
        viewModelScope.launch {
            try {
                // Invalidate cache to get fresh recommendations on each load
                recommendationService.invalidateCache()
                val recs = recommendationService.getPersonalizedRecommendations()

                // Чергуємо фільми і серіали для різноманітності стрічки
                val allRecs = buildList {
                    val movies = recs.movies.iterator()
                    val tvShows = recs.tvShows.iterator()
                    while (movies.hasNext() || tvShows.hasNext()) {
                        if (movies.hasNext()) add(movies.next())
                        if (tvShows.hasNext()) add(tvShows.next())
                    }
                }

                _recommendations.value = allRecs
            } catch (_: Exception) {
                _recommendations.value = emptyList()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
