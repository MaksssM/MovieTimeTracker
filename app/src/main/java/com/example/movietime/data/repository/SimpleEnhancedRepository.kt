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

                // Calculate watch time from runtime
                val totalWatchTimeMinutes = watchedItems.sumOf { it.runtime ?: 0 }
                android.util.Log.d("SimpleEnhancedRepository", "Calculated totalWatchTimeMinutes: $totalWatchTimeMinutes from ${watchedItems.size} items")
                watchedItems.forEach { item ->
                    android.util.Log.d("SimpleEnhancedRepository", "  - ${item.title}: runtime=${item.runtime}, mediaType=${item.mediaType}, episodes=${item.totalEpisodes}, episodeRuntime=${item.episodeRuntime}")

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

                // Calculate this month watched (simple calculation for now)
                val thisMonthWatched = minOf(totalWatchedMovies + totalWatchedTvShows, 8)

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

    // Real recent activities
    fun getRecentActivities(): Flow<List<RecentActivityItem>> = flow {
        // Poll every 5 seconds or just emit once? 
        // Emitting once is safer to avoid endless loop if DB is slow. 
        // But the existing stats loop suggests polling is established pattern here.
        // Let's just emit once for now, and maybe re-fetch on resume in Fragment.
        val items = appRepository.getRecentActivity(limit = 10)
        emit(items)
    }

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
