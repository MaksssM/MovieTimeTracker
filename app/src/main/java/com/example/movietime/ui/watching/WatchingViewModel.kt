package com.example.movietime.ui.watching

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.db.WatchedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class WatchingViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _watchingContent = MutableLiveData<List<WatchedItem>>()
    val watchingContent: LiveData<List<WatchedItem>> = _watchingContent

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadWatchingContent() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val content = withContext(Dispatchers.IO) {
                    repository.getWatchingContentSync()
                }
                _watchingContent.value = content
                _errorMessage.value = null
                Log.d("WatchingViewModel", "Loaded ${content.size} watching items")
            } catch (e: Exception) {
                Log.e("WatchingViewModel", "Error loading watching content", e)
                _watchingContent.value = emptyList()
                _errorMessage.value = "Помилка при завантаженні контенту: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToWatching(item: WatchedItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.addToWatching(item)
                }
                loadWatchingContent() // Reload to update the list
                Log.d("WatchingViewModel", "Added item to watching: ${item.title}")
            } catch (e: Exception) {
                Log.e("WatchingViewModel", "Error adding to watching", e)
                _errorMessage.value = "Помилка при додаванні: ${e.localizedMessage}"
            }
        }
    }

    fun removeFromWatching(item: WatchedItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.removeFromWatching(item.id, item.mediaType)
                }
                loadWatchingContent() // Reload to update the list
                Log.d("WatchingViewModel", "Removed item from watching: ${item.title}")
            } catch (e: Exception) {
                Log.e("WatchingViewModel", "Error removing from watching", e)
                _errorMessage.value = "Помилка при видаленні: ${e.localizedMessage}"
            }
        }
    }

    fun moveToWatched(item: WatchedItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Remove from watching and add to watched
                    repository.removeFromWatching(item.id, item.mediaType)
                    repository.addWatchedItem(item)
                }
                loadWatchingContent() // Reload to update the list
                Log.d("WatchingViewModel", "Moved item to watched: ${item.title}")
            } catch (e: Exception) {
                Log.e("WatchingViewModel", "Error moving to watched", e)
                _errorMessage.value = "Помилка при переміщенні: ${e.localizedMessage}"
            }
        }
    }

    fun getWatchingMoviesCount(): Int {
        return _watchingContent.value?.count { it.mediaType == "movie" } ?: 0
    }

    fun getWatchingTvShowsCount(): Int {
        return _watchingContent.value?.count { it.mediaType == "tv" } ?: 0
    }

    fun getTotalWatchingCount(): Int {
        return _watchingContent.value?.size ?: 0
    }
}
