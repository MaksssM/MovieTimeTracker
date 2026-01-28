package com.example.movietime.ui.today

import androidx.lifecycle.*
import com.example.movietime.data.model.*
import com.example.movietime.data.repository.TodayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val todayRepository: TodayRepository
) : ViewModel() {

    private val _digest = MutableLiveData<TodayDigest>()
    val digest: LiveData<TodayDigest> = _digest

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedSection = MutableLiveData<TodaySection>(TodaySection.ALL)
    val selectedSection: LiveData<TodaySection> = _selectedSection

    init {
        loadTodayDigest()
    }

    fun loadTodayDigest() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val digest = todayRepository.getTodayDigest()
                _digest.value = digest
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadTodayDigest()
    }

    fun selectSection(section: TodaySection) {
        _selectedSection.value = section
    }

    // Filtered data based on selected section
    val filteredNewEpisodes: LiveData<List<NewEpisodeItem>> = MediatorLiveData<List<NewEpisodeItem>>().apply {
        addSource(_digest) { digest ->
            value = when (_selectedSection.value) {
                TodaySection.EPISODES, TodaySection.ALL -> digest?.newEpisodes ?: emptyList()
                else -> emptyList()
            }
        }
        addSource(_selectedSection) { section ->
            value = when (section) {
                TodaySection.EPISODES, TodaySection.ALL -> _digest.value?.newEpisodes ?: emptyList()
                else -> emptyList()
            }
        }
    }

    val filteredReleases: LiveData<List<TodayReleaseItem>> = MediatorLiveData<List<TodayReleaseItem>>().apply {
        addSource(_digest) { digest ->
            value = when (_selectedSection.value) {
                TodaySection.RELEASES, TodaySection.ALL -> digest?.todayReleases ?: emptyList()
                else -> emptyList()
            }
        }
        addSource(_selectedSection) { section ->
            value = when (section) {
                TodaySection.RELEASES, TodaySection.ALL -> _digest.value?.todayReleases ?: emptyList()
                else -> emptyList()
            }
        }
    }

    val filteredUpcoming: LiveData<List<TodayReleaseItem>> = MediatorLiveData<List<TodayReleaseItem>>().apply {
        addSource(_digest) { digest ->
            value = when (_selectedSection.value) {
                TodaySection.UPCOMING, TodaySection.ALL -> digest?.upcomingThisWeek ?: emptyList()
                else -> emptyList()
            }
        }
        addSource(_selectedSection) { section ->
            value = when (section) {
                TodaySection.UPCOMING, TodaySection.ALL -> _digest.value?.upcomingThisWeek ?: emptyList()
                else -> emptyList()
            }
        }
    }

    val continueWatching: LiveData<List<ContinueWatchingItem>> = _digest.map { 
        it?.continueWatching ?: emptyList() 
    }

    val personalTips: LiveData<List<PersonalTip>> = _digest.map { 
        it?.personalTips ?: emptyList() 
    }

    // Statistics for the header
    val newEpisodesCount: LiveData<Int> = _digest.map { it?.newEpisodes?.size ?: 0 }
    val releasesCount: LiveData<Int> = _digest.map { it?.todayReleases?.size ?: 0 }
    val upcomingCount: LiveData<Int> = _digest.map { it?.upcomingThisWeek?.size ?: 0 }
}

enum class TodaySection {
    ALL,
    EPISODES,
    RELEASES,
    UPCOMING
}
