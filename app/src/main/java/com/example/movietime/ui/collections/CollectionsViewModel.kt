package com.example.movietime.ui.collections

import androidx.lifecycle.*
import com.example.movietime.data.db.UserCollection
import com.example.movietime.data.repository.CollectionWithCount
import com.example.movietime.data.repository.CollectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: CollectionsRepository
) : ViewModel() {

    private val _collections = MutableLiveData<List<CollectionWithCount>>()
    val collections: LiveData<List<CollectionWithCount>> = _collections

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadCollections()
    }

    fun loadCollections() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // We use getCollectionsWithCounts directly to get items count and preview posters
                val result = repository.getCollectionsWithCounts()
                _collections.value = result
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCollection(name: String, description: String?) {
        viewModelScope.launch {
            try {
                // Assign a random default emoji or let user pick (later)
                val defaultEmojis = listOf("🎬", "🍿", "⭐", "❤️", "🔥", "📺", "📂")
                val emoji = defaultEmojis.random()
                
                repository.createCollection(name, description, emoji)
                loadCollections() // Refresh list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCollection(id)
                loadCollections()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
