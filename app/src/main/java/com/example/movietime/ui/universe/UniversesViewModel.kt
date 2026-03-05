package com.example.movietime.ui.universe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.repository.UniverseRepository
import com.example.movietime.data.repository.UniverseWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniversesViewModel @Inject constructor(
    private val repository: UniverseRepository
) : ViewModel() {

    private val _universes = MutableLiveData<List<UniverseWithProgress>>()
    val universes: LiveData<List<UniverseWithProgress>> = _universes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUniverses()
    }

    fun loadUniverses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.ensureSeedData()
                _universes.value = repository.getUniversesWithProgress()
            } catch (e: Exception) {
                _universes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
