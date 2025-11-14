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

    fun loadMovie(movieId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getMovieDetails(movieId)
                _item.value = response
            } catch (e: Exception) {
                _item.value = null
            }
        }
    }

    fun loadTvShow(tvId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getTvShowDetails(tvId)
                _item.value = response
            } catch (e: Exception) {
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
                repository.addWatchedItem(item)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

}