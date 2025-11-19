package com.example.movietime.data.repository

import androidx.lifecycle.asFlow
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.model.*
import com.example.movietime.util.DateTimeUtils
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
            try {
                val totalWatchedMovies = watchedItems.count { it.mediaType == "movie" }
                val totalWatchedTvShows = watchedItems.count { it.mediaType == "tv" }

                // Calculate watch time from runtime
                val totalWatchTimeMinutes = watchedItems.sumOf { it.runtime ?: 0 }

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

    // Mock activities for demonstration
    fun getRecentActivities(): Flow<List<Activity>> = flow {
        val activities = listOf(
            Activity(
                id = "1",
                userId = "user1",
                username = "Ви",
                type = ActivityType.WATCHED_MOVIE,
                movieId = 550,
                movieTitle = "Бійцівський клуб",
                moviePoster = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                rating = 9.0f,
                createdDate = DateTimeUtils.getCurrentDateTime()
            ),
            Activity(
                id = "2",
                userId = "user1",
                username = "Ви",
                type = ActivityType.ADDED_TO_PLANNED,
                movieId = 157336,
                movieTitle = "Інтерстеллар",
                moviePoster = "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                createdDate = DateTimeUtils.getCurrentDateTime()
            )
        )
        emit(activities)
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
