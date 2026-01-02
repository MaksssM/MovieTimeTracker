package com.example.movietime.ui.collections

import androidx.lifecycle.*
import com.example.movietime.data.db.CollectionItem
import com.example.movietime.data.db.UserCollection
import com.example.movietime.data.repository.CollectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val repository: CollectionsRepository
) : ViewModel() {

    private val _collectionId = MutableLiveData<Long>()
    
    val collection: LiveData<UserCollection?> = _collectionId.switchMap { id ->
        repository.getCollectionByIdLive(id)
    }

    val items: LiveData<List<CollectionItem>> = _collectionId.switchMap { id ->
        repository.getItemsForCollection(id)
    }

    fun setCollectionId(id: Long) {
        _collectionId.value = id
    }

    fun removeItem(itemId: Int, mediaType: String) {
        val id = _collectionId.value ?: return
        viewModelScope.launch {
            repository.removeItemFromCollection(id, itemId, mediaType)
        }
    }

    fun deleteCollection() {
        val id = _collectionId.value ?: return
        viewModelScope.launch {
            repository.deleteCollection(id)
        }
    }
    
    fun updateCollection(name: String, description: String?) {
        val current = collection.value ?: return
        viewModelScope.launch {
            repository.updateCollection(current.copy(name = name, description = description))
        }
    }
}
