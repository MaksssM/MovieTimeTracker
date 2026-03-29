package com.example.movietime.ui.universe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.repository.EntryProgress
import com.example.movietime.data.repository.SagaWithEntries
import com.example.movietime.data.repository.UniverseRepository
import com.example.movietime.data.repository.UniverseWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniverseDetailViewModel @Inject constructor(
    private val repository: UniverseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val universeId: Long = checkNotNull(savedStateHandle[UniverseDetailActivity.EXTRA_UNIVERSE_ID])

    private val _sagas = MutableLiveData<List<SagaWithEntries>>()
    val sagas: LiveData<List<SagaWithEntries>> = _sagas

    private val _uncategorized = MutableLiveData<List<EntryProgress>>()
    val uncategorized: LiveData<List<EntryProgress>> = _uncategorized

    private val _universe = MutableLiveData<UniverseWithProgress?>()
    val universe: LiveData<UniverseWithProgress?> = _universe

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val universes = repository.getUniversesWithProgress()
                _universe.value = universes.firstOrNull { it.universe.id == universeId }
                _sagas.value = repository.getSagasWithEntries(universeId)
                _uncategorized.value = repository.getUncategorizedEntries(universeId)
            } catch (e: Exception) {
                _sagas.value = emptyList()
                _uncategorized.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
