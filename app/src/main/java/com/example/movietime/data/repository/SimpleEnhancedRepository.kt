package com.example.movietime.data.repository

import androidx.lifecycle.asFlow
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("unused")
class SimpleEnhancedRepository @Inject constructor(
    private val api: TmdbApi,
    private val appRepository: AppRepository,
    private val apiKey: String
) {

    // Real data from database using LiveData flow
    fun getDetailedStatistics(): Flow<DetailedStatistics> =
        appRepository.getWatchedItems().asFlow().map { watchedItems ->
            android.util.Log.d("SimpleEnhancedRepository", "getDetailedStatistics: got ${watchedItems.size} watched items")
            try {
                val totalWatchedMovies = watchedItems.count { it.mediaType == "movie" }
                val totalWatchedTvShows = watchedItems.count { it.mediaType == "tv" }

                // Calculate watch time from runtime, multiplying by watchCount for rewatches
                val totalWatchTimeMinutes = watchedItems.sumOf { (it.runtime ?: 0) * it.watchCount }
                android.util.Log.d("SimpleEnhancedRepository", "Calculated totalWatchTimeMinutes: $totalWatchTimeMinutes from ${watchedItems.size} items")
                watchedItems.forEach { item ->
                    android.util.Log.d("SimpleEnhancedRepository", "  - ${item.title}: runtime=${item.runtime}, watchCount=${item.watchCount}, total=${(item.runtime ?: 0) * item.watchCount}, mediaType=${item.mediaType}, episodes=${item.totalEpisodes}, episodeRuntime=${item.episodeRuntime}")

                    // Перевірка: якщо runtime null або 0 для серіалу, спробуємо перерахувати
                    if (item.mediaType == "tv" && (item.runtime == null || item.runtime == 0)) {
                        val calculatedRuntime = (item.totalEpisodes ?: 0) * (item.episodeRuntime ?: 45)
                        android.util.Log.w("SimpleEnhancedRepository", "  ⚠️ TV show '${item.title}' has null/zero runtime! Calculated: $calculatedRuntime (${item.totalEpisodes} eps × ${item.episodeRuntime} min)")
                    }
                }

                // Calculate average user rating from items with ratings
                val itemsWithRatings = watchedItems.filter { it.userRating != null && it.userRating > 0 }
                val averageUserRating = if (itemsWithRatings.isNotEmpty()) {
                    itemsWithRatings.map { it.userRating!! }.average().toFloat()
                } else {
                    0f
                }

                // Calculate this month watched - count actions this month (new watches + rewatches)
                val calendar = java.util.Calendar.getInstance()
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val currentYear = calendar.get(java.util.Calendar.YEAR)

                calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                calendar.set(java.util.Calendar.MINUTE, 59)
                calendar.set(java.util.Calendar.SECOND, 59)
                calendar.set(java.util.Calendar.MILLISECOND, 999)
                val endOfMonth = calendar.timeInMillis

                val itemsThisMonth = watchedItems.sumOf { item ->
                    val lastUpdated = item.lastUpdated
                    if (lastUpdated != null && lastUpdated in startOfMonth..endOfMonth) {
                        maxOf(item.watchCount, 1)
                    } else 0
                }

                val thisMonthWatched = itemsThisMonth

                android.util.Log.d("SimpleEnhancedRepository", "Total this month (items+rewatches): $thisMonthWatched")

                DetailedStatistics(
                    totalWatchedMovies = totalWatchedMovies,
                    totalWatchedTvShows = totalWatchedTvShows,
                    totalPlannedMovies = 0,
                    totalPlannedTvShows = 0,
                    totalWatchingMovies = 0,
                    totalWatchingTvShows = 0,
                    totalWatchTimeMinutes = totalWatchTimeMinutes,
                    averageUserRating = averageUserRating,
                    thisMonthWatched = thisMonthWatched,
                    thisYearWatched = totalWatchedMovies + totalWatchedTvShows
                )
            } catch (e: Exception) {
                // Fallback to empty stats if there's an error
                DetailedStatistics(
                    totalWatchedMovies = 0,
                    totalWatchedTvShows = 0,
                    totalPlannedMovies = 0,
                    totalPlannedTvShows = 0,
                    totalWatchingMovies = 0,
                    totalWatchingTvShows = 0,
                    totalWatchTimeMinutes = 0,
                    averageUserRating = 0f,
                    thisMonthWatched = 0,
                    thisYearWatched = 0
                )
            }
        }.combine(getPlannedAndWatchingCounts()) { stats, counts ->
            stats.copy(
                totalPlannedMovies = counts.plannedMovies,
                totalPlannedTvShows = counts.plannedTvShows,
                totalWatchingMovies = counts.watchingMovies,
                totalWatchingTvShows = counts.watchingTvShows
            )
        }

    private fun getPlannedAndWatchingCounts(): Flow<ContentCounts> = flow {
        while (true) {
            try {
                val plannedMovies = appRepository.getPlannedMoviesCount()
                val plannedTvShows = appRepository.getPlannedTvShowsCount()
                val watchingMovies = appRepository.getWatchingMoviesCount()
                val watchingTvShows = appRepository.getWatchingTvShowsCount()

                emit(ContentCounts(plannedMovies, plannedTvShows, watchingMovies, watchingTvShows))
            } catch (e: Exception) {
                emit(ContentCounts(0, 0, 0, 0))
            }
            kotlinx.coroutines.delay(2000) // Update every 2 seconds
        }
    }

    private data class ContentCounts(
        val plannedMovies: Int,
        val plannedTvShows: Int,
        val watchingMovies: Int,
        val watchingTvShows: Int
    )

    // Real recent activities - polls every 3 seconds for updates
    fun getRecentActivities(): Flow<List<RecentActivityItem>> = flow {
        android.util.Log.d("SimpleEnhancedRepository", "getRecentActivities: Starting flow")
        while (true) {
            try {
                android.util.Log.d("SimpleEnhancedRepository", "getRecentActivities: Fetching items...")
                val items = appRepository.getRecentActivity(limit = 10)
                android.util.Log.d("SimpleEnhancedRepository", "getRecentActivities: Got ${items.size} items")
                items.forEach { item ->
                    android.util.Log.d("SimpleEnhancedRepository", "  - ${item.title} (${item.type})")
                }
                emit(items)
            } catch (e: Exception) {
                android.util.Log.e("SimpleEnhancedRepository", "getRecentActivities: Error - ${e.message}", e)
                emit(emptyList())
            }
            kotlinx.coroutines.delay(3000)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun getTrendingContent(timeWindow: String = "week"): Result<String?> {
        return try {
            // Use existing API to get popular movies for background
            val response = api.getPopularMovies(apiKey)
            val movies = response.results
            val randomBackdrop = movies.filter { it.backdropPath != null }
                .randomOrNull()?.backdropPath
            Result.success(randomBackdrop)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularMovies(): Result<List<MovieResult>> {
        return try {
            val response = api.getPopularMovies(apiKey)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
