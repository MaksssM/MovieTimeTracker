package com.example.movietime.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.movietime.util.Utils
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
        val details = list.map { "${it.id}:${it.mediaType}:${it.runtime ?: 0}" }
        Log.d("MainViewModel", "watchedList updated: count=${list.size}, items=$details")
        Log.d("MainViewModel", "computed totalMinutes = $sum")
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
}