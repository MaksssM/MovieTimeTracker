package com.example.movietime.ui.statistics

import android.content.Context
import androidx.lifecycle.*
import com.example.movietime.R
import com.example.movietime.data.model.ActorStatItem
import com.example.movietime.data.model.DetailedStatistics
import com.example.movietime.data.model.DirectorStatItem
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _actorsLoading = MutableLiveData<Boolean>()
    val actorsLoading: LiveData<Boolean> = _actorsLoading

    // Cache for director & actor data
    private val directorCache = mutableMapOf<Int, DirectorStatItem>()
    private val actorCache = mutableMapOf<Int, ActorStatItem>()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // First paint: instant stats from local DB only (no network).
                val stats = statisticsRepository.getDetailedStatistics(directorCache)
                val enrichedStats = if (actorCache.isNotEmpty()) {
                    stats.copy(
                        favoriteActors = actorCache.values.sortedByDescending { it.moviesWatched }.take(10)
                    )
                } else {
                    stats
                }
                _statistics.value = enrichedStats

                // Load directors and actors in background if not cached
                if (directorCache.isEmpty() || actorCache.isEmpty()) {
                    loadCastAndCrew()
                }

                // Backfill missing genreIds in background, then recompute once.
                // Runs AFTER first paint so a slow network never blocks the screen.
                backfillMissingGenreIds { fixedCount ->
                    if (fixedCount > 0) {
                        refreshStatsOnly()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshStatsOnly() {
        viewModelScope.launch {
            try {
                val stats = statisticsRepository.getDetailedStatistics(directorCache)
                val enriched = if (actorCache.isNotEmpty()) {
                    stats.copy(
                        favoriteActors = actorCache.values.sortedByDescending { it.moviesWatched }.take(10)
                    )
                } else {
                    stats
                }
                _statistics.value = enriched
            } catch (_: Exception) {
            }
        }
    }

    private fun loadCastAndCrew() {
        viewModelScope.launch {
            _directorsLoading.value = true
            _actorsLoading.value = true
            try {
                // Movies AND tv shows (cartoons/anime included — same credits API).
                // Title/runtime come straight from WatchedItem: no extra details calls.
                val allWatched = withContext(Dispatchers.IO) {
                    appRepository.getWatchedItemsForBackup()
                }
                val itemsToFetch = allWatched.take(60)

                val newDirectorCounts = mutableMapOf<Int, MutableList<Pair<String, Int>>>()
                val newDirectorInfo = mutableMapOf<Int, Pair<String, String?>>()
                val newActorCounts = mutableMapOf<Int, MutableList<Pair<String, Int>>>()
                val newActorInfo = mutableMapOf<Int, Pair<String, String?>>()
                val lock = Any()

                withContext(Dispatchers.IO) {
                    // Bounded parallelism: chunks of 8 instead of 100 sequential calls
                    itemsToFetch.chunked(8).forEach { chunk ->
                        chunk.map { item ->
                            async {
                                try {
                                    val credits = if (item.mediaType == "tv") {
                                        appRepository.getTvCredits(item.id)
                                    } else {
                                        appRepository.getMovieCredits(item.id)
                                    } ?: return@async
                                    val runtime = item.runtime ?: 0
                                    val title = item.title

                                    // 1. Directors (series have per-episode/series directors)
                                    credits.crew
                                        ?.filter { it.job == "Director" || it.job == "Series Director" }
                                        ?.forEach { director ->
                                            synchronized(lock) {
                                                newDirectorInfo[director.id] =
                                                    Pair(director.name, director.profilePath)
                                                newDirectorCounts.getOrPut(director.id) { mutableListOf() }
                                                    .add(Pair(title, runtime))
                                            }
                                        }

                                    // 2. Actors (top 5 cast)
                                    credits.cast
                                        ?.take(5)
                                        ?.forEach { cast ->
                                            synchronized(lock) {
                                                newActorInfo[cast.id] =
                                                    Pair(cast.name, cast.profilePath)
                                                newActorCounts.getOrPut(cast.id) { mutableListOf() }
                                                    .add(Pair(title, runtime))
                                            }
                                        }
                                } catch (_: Exception) {
                                    // Skip failed individual requests
                                }
                            }
                        }.awaitAll()
                    }
                }

                // Swap caches only after successful build (no wipe-on-failure)
                if (newDirectorCounts.isNotEmpty() || newActorCounts.isNotEmpty()) {
                    directorCache.clear()
                    newDirectorCounts.forEach { (directorId, movies) ->
                        val info = newDirectorInfo[directorId] ?: return@forEach
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

                    actorCache.clear()
                    newActorCounts.forEach { (actorId, movies) ->
                        val info = newActorInfo[actorId] ?: return@forEach
                        val totalRuntime = movies.sumOf { it.second.toLong() }

                        actorCache[actorId] = ActorStatItem(
                            actorId = actorId,
                            actorName = info.first,
                            profilePath = info.second,
                            moviesWatched = movies.size,
                            totalWatchTimeMinutes = totalRuntime,
                            movieTitles = movies.map { it.first }
                        )
                    }
                }

                // Refresh statistics with director and actor data
                val updatedStats = statisticsRepository.getDetailedStatistics(directorCache)
                val sortedActors = actorCache.values
                    .sortedWith(compareByDescending<ActorStatItem> { it.moviesWatched }.thenByDescending { it.totalWatchTimeMinutes })
                    .take(10)

                _statistics.value = updatedStats.copy(
                    favoriteActors = sortedActors
                )

            } catch (e: Exception) {
                // Cast/crew loading failed, but we still have core stats
            } finally {
                _directorsLoading.value = false
                _actorsLoading.value = false
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

    /**
     * Fills genreIds for watched rows saved without them (legacy rows and
     * TV rows from the episode sheet). Capped and chunked to stay fast;
     * details responses are LRU-cached in the repository.
     * Reports how many rows were fixed via [onDone] (0 = nothing changed).
     */
    private suspend fun backfillMissingGenreIds(onDone: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        var fixedCount = 0
        try {
            val missing = appRepository.getWatchedItemsForBackup()
                .filter { it.genreIds.isNullOrBlank() }
                .take(80)
            if (missing.isEmpty()) return@withContext
            missing.chunked(8).forEach { chunk ->
                chunk.map { item ->
                    async {
                        try {
                            val genres: String? = if (item.mediaType == "tv") {
                                val details = appRepository.getTvShowDetails(item.id)
                                details.genres?.map { it.id }?.joinToString(",")
                                    ?: details.genreIds?.joinToString(",")
                            } else {
                                val details = appRepository.getMovieDetails(item.id)
                                details.genres?.map { it.id }?.joinToString(",")
                                    ?: details.genreIds?.joinToString(",")
                            }
                            if (!genres.isNullOrBlank()) {
                                appRepository.updateWatchedItem(item.copy(genreIds = genres))
                                fixedCount++
                            }
                        } catch (_: Exception) {
                        }
                    }
                }.awaitAll()
            }
            onDone(fixedCount)
        } catch (_: Exception) {
            onDone(0)
        }
    }
}
