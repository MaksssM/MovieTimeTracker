package com.example.movietime.ui.statistics

import android.content.Context
import androidx.lifecycle.*
import com.example.movietime.R
import com.example.movietime.data.model.DetailedStatistics
import com.example.movietime.data.model.DirectorStatItem
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statisticsRepository: StatisticsRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _statistics = MutableLiveData<DetailedStatistics>()
    val statistics: LiveData<DetailedStatistics> = _statistics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _directorsLoading = MutableLiveData<Boolean>()
    val directorsLoading: LiveData<Boolean> = _directorsLoading

    // Cache for director data
    private val directorCache = mutableMapOf<Int, DirectorStatItem>()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val stats = statisticsRepository.getDetailedStatistics(directorCache)
                _statistics.value = stats
                
                // Load directors in background if not cached
                if (directorCache.isEmpty()) {
                    loadDirectors()
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadDirectors() {
        viewModelScope.launch {
            _directorsLoading.value = true
            try {
                val movieIds = statisticsRepository.getWatchedMovieIds()
                val directorCounts = mutableMapOf<Int, MutableList<Pair<String, Int>>>() // directorId -> list of (movieTitle, runtime)
                val directorInfo = mutableMapOf<Int, Pair<String, String?>>() // directorId -> (name, profilePath)
                
                // Fetch credits for each movie (limit to avoid too many API calls)
                val moviesToFetch = movieIds.take(50)
                
                withContext(Dispatchers.IO) {
                    moviesToFetch.forEach { movieId ->
                        try {
                            val credits = appRepository.getMovieCredits(movieId)
                            val movieDetails = appRepository.getMovieDetails(movieId)
                            
                            credits?.crew
                                ?.filter { it.job == "Director" }
                                ?.forEach { director ->
                                    val id = director.id
                                    val runtime = movieDetails.runtime ?: 0
                                    
                                    directorInfo[id] = Pair(director.name, director.profilePath)
                                    directorCounts.getOrPut(id) { mutableListOf() }
                                        .add(Pair(movieDetails.title ?: "Unknown", runtime))
                                }
                        } catch (e: Exception) {
                            // Skip failed requests
                        }
                    }
                }
                
                // Build director stats
                directorCache.clear()
                directorCounts.forEach { (directorId, movies) ->
                    val info = directorInfo[directorId] ?: return@forEach
                    val totalRuntime = movies.sumOf { it.second.toLong() }
                    
                    directorCache[directorId] = DirectorStatItem(
                        directorId = directorId,
                        directorName = info.first,
                        profilePath = info.second,
                        moviesWatched = movies.size,
                        totalWatchTimeMinutes = totalRuntime,
                        movieTitles = movies.map { it.first }
                    )
                }
                
                // Refresh statistics with director data
                val updatedStats = statisticsRepository.getDetailedStatistics(directorCache)
                _statistics.value = updatedStats
                
            } catch (e: Exception) {
                // Directors loading failed, but we still have other stats
            } finally {
                _directorsLoading.value = false
            }
        }
    }

    fun formatWatchTime(minutes: Long): String {
        return when {
            minutes < 60 -> context.getString(R.string.time_format_minutes, minutes.toInt())
            minutes < 1440 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) context.getString(R.string.time_format_hours_minutes, hours.toInt(), mins.toInt())
                else context.getString(R.string.time_format_hours, hours.toInt())
            }
            else -> {
                val days = minutes / 1440
                val hours = (minutes % 1440) / 60
                if (hours > 0) context.getString(R.string.time_format_days_hours, days.toInt(), hours.toInt())
                else context.getString(R.string.time_format_days, days.toInt())
            }
        }
    }

    fun formatWatchTimeShort(minutes: Long): String {
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days >= 1 -> context.getString(R.string.time_format_days, days.toInt())
            hours >= 1 -> context.getString(R.string.time_format_hours, hours.toInt())
            else -> context.getString(R.string.time_format_minutes, minutes.toInt())
        }
    }

    fun refreshData() {
        directorCache.clear()
        loadStatistics()
    }
}
