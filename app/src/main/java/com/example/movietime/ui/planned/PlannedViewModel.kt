package com.example.movietime.ui.planned

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.db.WatchedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class PlannedViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _plannedContent = MutableLiveData<List<WatchedItem>>()
    val plannedContent: LiveData<List<WatchedItem>> = _plannedContent

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadPlannedContent() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val content = repository.getPlannedContentSync()
                _plannedContent.value = content
                _errorMessage.value = null
                Log.d("PlannedViewModel", "Loaded ${content.size} planned items")
            } catch (e: Exception) {
                Log.e("PlannedViewModel", "Error loading planned content", e)
                _plannedContent.value = emptyList()
                _errorMessage.value = "Помилка при завантаженні запланованого контенту: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToPlanned(item: WatchedItem) {
        viewModelScope.launch {
            try {
                repository.addToPlanned(item)
                loadPlannedContent() // Reload to update the list
                Log.d("PlannedViewModel", "Added item to planned: ${item.title}")
            } catch (e: Exception) {
                Log.e("PlannedViewModel", "Error adding to planned", e)
                _errorMessage.value = "Помилка при додаванні до запланованих: ${e.localizedMessage}"
            }
        }
    }

    fun removeFromPlanned(item: WatchedItem) {
        viewModelScope.launch {
            try {
                repository.removeFromPlanned(item.id, item.mediaType)
                loadPlannedContent() // Reload to update the list
                Log.d("PlannedViewModel", "Removed item from planned: ${item.title}")
            } catch (e: Exception) {
                Log.e("PlannedViewModel", "Error removing from planned", e)
                _errorMessage.value = "Помилка при видаленні з запланованих: ${e.localizedMessage}"
            }
        }
    }

    fun moveToWatched(item: WatchedItem) {
        viewModelScope.launch {
            try {
                // Remove from planned and add to watched
                repository.removeFromPlanned(item.id, item.mediaType)
                repository.addWatchedItem(item)
                loadPlannedContent() // Reload to update the list
                Log.d("PlannedViewModel", "Moved item to watched: ${item.title}")
            } catch (e: Exception) {
                Log.e("PlannedViewModel", "Error moving to watched", e)
                _errorMessage.value = "Помилка при переміщенні: ${e.localizedMessage}"
            }
        }
    }

    fun getPlannedMoviesCount(): Int {
        return _plannedContent.value?.count { it.mediaType == "movie" } ?: 0
    }

    fun getPlannedTvShowsCount(): Int {
        return _plannedContent.value?.count { it.mediaType == "tv" } ?: 0
    }

    fun getTotalPlannedCount(): Int {
        return _plannedContent.value?.size ?: 0
    }
}
