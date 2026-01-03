package com.example.movietime.ui.statistics

import androidx.lifecycle.*
import com.example.movietime.data.db.YearlyStats
import com.example.movietime.data.repository.LifetimeStats
import com.example.movietime.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class YearInReviewViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _yearlyStats = MutableLiveData<YearlyStats?>()
    val yearlyStats: LiveData<YearlyStats?> = _yearlyStats

    private val _lifetimeStats = MutableLiveData<LifetimeStats>()
    val lifetimeStats: LiveData<LifetimeStats> = _lifetimeStats

    private val _availableYears = MutableLiveData<List<Int>>()
    val availableYears: LiveData<List<Int>> = _availableYears

    private val _yearlyTrend = MutableLiveData<List<YearlyStats>>(emptyList())
    val yearlyTrend: LiveData<List<YearlyStats>> = _yearlyTrend

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedYear = MutableLiveData<Int>()
    val selectedYear: LiveData<Int> = _selectedYear

    init {
        // Default to current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        _selectedYear.value = currentYear
        loadStatsForYear(currentYear)
        loadAvailableYears()
        loadLifetimeStats()
        loadYearlyTrend()
    }

    fun loadStatsForYear(year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedYear.value = year
            try {
                val stats = statisticsRepository.getYearlyStats(year)
                _yearlyStats.value = stats
            } catch (e: Exception) {
                _yearlyStats.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recalculateStats(year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stats = statisticsRepository.calculateYearlyStats(year)
                _yearlyStats.value = stats
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            val years = statisticsRepository.getAvailableYears()
            _availableYears.value = years.ifEmpty {
                listOf(Calendar.getInstance().get(Calendar.YEAR))
            }
        }
    }

    private fun loadLifetimeStats() {
        viewModelScope.launch {
            val stats = statisticsRepository.getLifetimeStats()
            _lifetimeStats.value = stats
        }
    }

    private fun loadYearlyTrend() {
        viewModelScope.launch {
            try {
                _yearlyTrend.value = statisticsRepository.getAllYearlyStats().sortedBy { it.year }
            } catch (_: Exception) {
                _yearlyTrend.value = emptyList()
            }
        }
    }

    fun formatWatchTime(minutes: Long): String {
        return when {
            minutes < 60 -> "$minutes хв"
            minutes < 1440 -> "${minutes / 60} год ${minutes % 60} хв"
            else -> {
                val days = minutes / 1440
                val hours = (minutes % 1440) / 60
                "$days днів $hours год"
            }
        }
    }

    fun formatWatchTimeEquivalent(minutes: Long): String {
        val hours = minutes / 60.0
        val days = hours / 24.0
        
        return when {
            days >= 1 -> "Це %.1f днів без перерви!".format(days)
            hours >= 1 -> "Це %.1f годин без перерви!".format(hours)
            else -> "Це $minutes хвилин!"
        }
    }
}
