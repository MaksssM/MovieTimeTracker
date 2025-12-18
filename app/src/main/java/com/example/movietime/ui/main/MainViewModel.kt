package com.example.movietime.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.movietime.util.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val watchedList: LiveData<List<WatchedItem>> = repository.getWatchedItems().also {
        // no-op here; logging below on mapped streams
    }

    val listIsEmpty: LiveData<Boolean> = watchedList.map { it.isEmpty() }

    // New: total minutes across all watched items (treat null runtime as 0)
    val totalMinutes: LiveData<Int> = watchedList.map { list ->
        val sum = list.sumOf { it.runtime ?: 0 }
        // Log details for debugging
        val details = list.map { item ->
            val runtimeStr = if (item.runtime == null) "null" else item.runtime.toString()
            "${item.title}(id:${item.id},type:${item.mediaType},runtime:$runtimeStr)"
        }

        val movieCount = list.count { it.mediaType == "movie" }
        val tvCount = list.count { it.mediaType == "tv" }
        val movieRuntime = list.filter { it.mediaType == "movie" }.sumOf { it.runtime ?: 0 }
        val tvRuntime = list.filter { it.mediaType == "tv" }.sumOf { it.runtime ?: 0 }

        Log.d("MainViewModel", "watchedList updated: total count=${list.size}, movies=$movieCount, tv=$tvCount")
        Log.d("MainViewModel", "Runtime breakdown: movies=$movieRuntime min, tv=$tvRuntime min, total=$sum min")
        Log.d("MainViewModel", "Items: $details")
        sum
    }

    // New: formatted total time for display (e.g. "2 год 30 хв")
    val totalTimeFormatted: LiveData<String> = totalMinutes.map { minutes ->
        val formatted = Utils.formatMinutesToHoursAndMinutes(minutes)
        Log.d("MainViewModel", "totalTimeFormatted = $formatted")
        formatted
    }

    // Add watched item (used for undo)
    fun addWatchedItem(item: WatchedItem) {
        viewModelScope.launch {
            try {
                repository.addWatchedItem(item)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to add watched item: ${e.message}")
            }
        }
    }

    // Delete watched item
    fun deleteWatchedItem(item: WatchedItem) {
        viewModelScope.launch {
            try {
                repository.deleteWatchedItem(item)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to delete watched item: ${e.message}")
            }
        }
    }

    private val _statistics = MutableStateFlow(WatchedStatistics())

    fun getWatchedStatistics(): StateFlow<WatchedStatistics> = _statistics

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                // Load data from watched items only
                repository.getWatchedItems().asFlow().collect { watchedItems ->
                    val totalMinutes = watchedItems.sumOf { it.runtime ?: 0 }
                    val movieCount = watchedItems.count { it.mediaType == "movie" }
                    val tvShowCount = watchedItems.count { it.mediaType == "tv" }

                    val stats = WatchedStatistics(
                        totalMinutes = totalMinutes,
                        movieCount = movieCount,
                        tvShowCount = tvShowCount,
                        plannedMovieCount = 0, // Temporarily set to 0
                        plannedTvShowCount = 0  // Temporarily set to 0
                    )
                    _statistics.value = stats
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load statistics: ${e.message}")
            }
        }
    }
}