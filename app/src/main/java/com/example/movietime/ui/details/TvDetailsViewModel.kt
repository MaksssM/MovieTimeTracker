package com.example.movietime.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.db.WatchedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvDetailsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _tvShow = MutableLiveData<TvShowResult?>()
    val tvShow: LiveData<TvShowResult?> = _tvShow

    private val _watchedItem = MutableLiveData<WatchedItem?>()
    val watchedItem: LiveData<WatchedItem?> = _watchedItem

    fun loadTvShow(tvId: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("TvDetailsViewModel", "Loading TV show with ID: $tvId")
                val response = repository.getTvShowDetails(tvId)
                android.util.Log.d("TvDetailsViewModel", "TV show loaded successfully: ${response.name}, episodes: ${response.numberOfEpisodes}, seasons: ${response.numberOfSeasons}")
                _tvShow.value = response
                android.util.Log.d("TvDetailsViewModel", "LiveData updated with TV show data")
                
                // Also check if it's watched
                checkIfWatched(tvId)
            } catch (e: Exception) {
                android.util.Log.e("TvDetailsViewModel", "Failed to load TV show: ${e.message}", e)
                _tvShow.value = null
            }
        }
    }

    private fun checkIfWatched(id: Int) {
        viewModelScope.launch {
            try {
                // Assuming mediaType is "tv" since this is TvDetailsViewModel
                val item = repository.getWatchedItemById(id, "tv")
                _watchedItem.value = item
            } catch (e: Exception) {
                _watchedItem.value = null
            }
        }
    }

    fun isItemWatched(id: Int, mediaType: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val found = repository.getWatchedItemById(id, mediaType)
                callback(found != null)
                // Update LiveData as well
                _watchedItem.value = found
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun addWatchedItem(item: WatchedItem, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Remove from Planned if exists
                try {
                    repository.removeFromPlanned(item.id, item.mediaType)
                    android.util.Log.d("TvDetailsViewModel", "Removed from planned: ${item.id}")
                } catch (e: Exception) {
                    // Ignore if not in planned
                }
                
                // Remove from Watching if exists
                try {
                    repository.removeFromWatching(item.id, item.mediaType)
                    android.util.Log.d("TvDetailsViewModel", "Removed from watching: ${item.id}")
                } catch (e: Exception) {
                    // Ignore if not in watching
                }
                
                repository.addWatchedItem(item)
                android.util.Log.d("TvDetailsViewModel", "Successfully added to watched: ${item.id}")
                // Update LiveData
                _watchedItem.value = item
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("TvDetailsViewModel", "Failed to add watched: ${e.message}")
                callback(false)
            }
        }
    }
}