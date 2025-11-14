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

    fun loadTvShow(tvId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getTvShowDetails(tvId)
                _tvShow.value = response
            } catch (e: Exception) {
                _tvShow.value = null
            }
        }
    }

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