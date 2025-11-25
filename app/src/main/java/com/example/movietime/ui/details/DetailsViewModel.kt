package com.example.movietime.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.db.WatchedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // Unified holder for either MovieResult or TvShowResult
    private val _item = MutableLiveData<Any?>()
    val item: LiveData<Any?> = _item

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadMovie(movieId: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DetailsViewModel", "Loading movie with ID: $movieId")
                val response = repository.getMovieDetails(movieId)
                android.util.Log.d("DetailsViewModel", "Movie loaded successfully: ${response.title}, runtime=${response.runtime}")
                _item.value = response
            } catch (e: Exception) {
                android.util.Log.e("DetailsViewModel", "Failed to load movie: ${e.message}", e)
                _item.value = null
            }
        }
    }

    fun loadTvShow(tvId: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DetailsViewModel", "Loading TV show with ID: $tvId")
                val response = repository.getTvShowDetails(tvId)
                android.util.Log.d("DetailsViewModel", "TV show loaded successfully: ${response.name}")
                _item.value = response
            } catch (e: Exception) {
                android.util.Log.e("DetailsViewModel", "Failed to load TV show: ${e.message}", e)
                _item.value = null
            }
        }
    }

    // Check if watched item exists by id and mediaType
    fun isItemWatched(id: Int, mediaType: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val found = repository.getWatchedItemById(id, mediaType)
                callback(found != null)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    // Add watched item
    fun addWatchedItem(item: WatchedItem, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DetailsViewModel", "Adding watched item: id=${item.id}, title=${item.title}, mediaType=${item.mediaType}, runtime=${item.runtime}")
                repository.addWatchedItem(item)
                android.util.Log.d("DetailsViewModel", "Successfully added watched item: ${item.id}")
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("DetailsViewModel", "Failed to add watched item: ${e.message}", e)
                callback(false)
            }
        }
    }

    // Check if item exists in planned list
    fun isItemPlanned(id: Int, mediaType: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val found = repository.getPlannedItemById(id, mediaType)
                callback(found != null)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    // Add item to planned list
    fun addToPlanned(item: WatchedItem, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.addToPlanned(item)
                android.util.Log.d("DetailsViewModel", "Successfully added to planned: ${item.id}")
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("DetailsViewModel", "Failed to add to planned: ${e.message}", e)
                callback(false)
            }
        }
    }

    // Check if item exists in watching list
    fun isItemWatching(id: Int, mediaType: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val found = repository.getWatchingItemById(id, mediaType)
                callback(found != null)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    // Add item to watching list
    fun addToWatching(item: WatchedItem, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.addToWatching(item)
                android.util.Log.d("DetailsViewModel", "Successfully added to watching: ${item.id}")
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("DetailsViewModel", "Failed to add to watching: ${e.message}", e)
                callback(false)
            }
        }
    }

}