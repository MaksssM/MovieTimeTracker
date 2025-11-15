package com.example.movietime.data.repository

import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.*
import com.example.movietime.data.model.*
import com.example.movietime.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("unused")
class SimpleEnhancedRepository @Inject constructor(
    private val api: TmdbApi,
    @Suppress("unused") private val watchedDao: WatchedItemDao,
    private val apiKey: String
) {

    // Mock data for demonstration
    fun getDetailedStatistics(): Flow<DetailedStatistics> = flow {
        // Calculate basic statistics from watched items
        // For now, return mock data as placeholder
        emit(DetailedStatistics(
            totalWatchedMovies = 15,
            totalWatchedTvShows = 8,
            totalPlannedMovies = 5,
            totalPlannedTvShows = 3,
            totalWatchTimeMinutes = 7500, // 125 hours
            averageUserRating = 8.2f,
            thisMonthWatched = 8,
            thisYearWatched = 23
        ))
    }

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
