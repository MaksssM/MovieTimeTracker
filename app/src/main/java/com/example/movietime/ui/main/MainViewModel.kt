package com.example.movietime.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.formatMinutesToHoursAndMinutes
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val allWatchedItems: LiveData<List<WatchedItem>> = repository.allWatchedItems

    val totalWatchTimeFormatted: LiveData<String> = repository.totalWatchTime.map { totalMinutes ->
        formatMinutesToHoursAndMinutes(totalMinutes)
    }

    fun deleteWatchedItem(item: WatchedItem) {
        viewModelScope.launch {
            repository.deleteWatchedItem(item.id)
        }
    }

    fun addWatchedItem(item: WatchedItem) {
        viewModelScope.launch {
            repository.addWatchedItem(item)
        }
    }
}