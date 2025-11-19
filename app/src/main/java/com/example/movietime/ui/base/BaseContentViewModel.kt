package com.example.movietime.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Base ViewModel for content management (Planned, Watching)
 * Reduces code duplication and provides common functionality
 */
abstract class BaseContentViewModel(
    protected val repository: AppRepository
) : ViewModel() {

    private val _content = MutableLiveData<List<WatchedItem>>()
    val content: LiveData<List<WatchedItem>> = _content

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    protected abstract val logTag: String

    protected abstract suspend fun fetchContent(): List<WatchedItem>
    protected abstract suspend fun addItem(item: WatchedItem)
    protected abstract suspend fun removeItem(id: Int, mediaType: String)

    fun loadContent() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val items = fetchContent()
                _content.value = items
                _errorMessage.value = null
                Log.d(logTag, "Loaded ${items.size} items")
            } catch (e: Exception) {
                Log.e(logTag, "Error loading content", e)
                _content.value = emptyList()
                _errorMessage.value = "Помилка при завантаженні: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun add(item: WatchedItem) {
        viewModelScope.launch {
            try {
                addItem(item)
                loadContent()
                Log.d(logTag, "Added item: ${item.title}")
            } catch (e: Exception) {
                Log.e(logTag, "Error adding item", e)
                _errorMessage.value = "Помилка при додаванні: ${e.localizedMessage}"
            }
        }
    }

    fun remove(item: WatchedItem) {
        viewModelScope.launch {
            try {
                removeItem(item.id, item.mediaType)
                loadContent()
                Log.d(logTag, "Removed item: ${item.title}")
            } catch (e: Exception) {
                Log.e(logTag, "Error removing item", e)
                _errorMessage.value = "Помилка при видаленні: ${e.localizedMessage}"
            }
        }
    }

    fun getMoviesCount(): Int = _content.value?.count { it.mediaType == "movie" } ?: 0
    fun getTvShowsCount(): Int = _content.value?.count { it.mediaType == "tv" } ?: 0
    fun getTotalCount(): Int = _content.value?.size ?: 0
}

