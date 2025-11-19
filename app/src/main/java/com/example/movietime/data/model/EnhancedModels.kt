package com.example.movietime.data.model

import androidx.room.*
import com.example.movietime.util.DateTimeUtils

// Enhanced WatchedItem with rating and social features
@Entity(tableName = "watched_items")
data class WatchedItem(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val runtime: Int = 0,
    val isMovie: Boolean = true,
    val tmdbId: Int,
    val backdropPath: String? = null,
    @ColumnInfo(name = "watched_date") val watchedDate: String = DateTimeUtils.getCurrentDateTime(),
    @ColumnInfo(name = "user_rating") val userRating: Float? = null, // User's personal rating 0-10
    @ColumnInfo(name = "review") val review: String? = null, // User's review
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "rewatch_count") val rewatchCount: Int = 0
)

// New entity for planned items
@Entity(tableName = "planned_items")
data class PlannedItem(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val runtime: Int = 0,
    val isMovie: Boolean = true,
    val tmdbId: Int,
    val backdropPath: String? = null,
    @ColumnInfo(name = "added_date") val addedDate: String = DateTimeUtils.getCurrentDateTime(),
    @ColumnInfo(name = "priority") val priority: Int = 0, // 0 = low, 1 = medium, 2 = high
    @ColumnInfo(name = "expected_rating") val expectedRating: Float? = null,
    @ColumnInfo(name = "notes") val notes: String? = null
)

// Friends system
@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String? = null,
    @ColumnInfo(name = "added_date") val addedDate: String = DateTimeUtils.getCurrentDateTime(),
    @ColumnInfo(name = "is_mutual") val isMutual: Boolean = false,
    @ColumnInfo(name = "last_activity") val lastActivity: String? = null
)

// Friend requests
@Entity(tableName = "friend_requests")
data class FriendRequest(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "from_user_id") val fromUserId: String,
    @ColumnInfo(name = "to_user_id") val toUserId: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String? = null,
    @ColumnInfo(name = "request_date") val requestDate: String = DateTimeUtils.getCurrentDateTime(),
    val status: RequestStatus = RequestStatus.PENDING
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

// Shared activities for social features
@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val username: String,
    val type: ActivityType,
    @ColumnInfo(name = "movie_id") val movieId: Int? = null,
    @ColumnInfo(name = "movie_title") val movieTitle: String? = null,
    @ColumnInfo(name = "movie_poster") val moviePoster: String? = null,
    val rating: Float? = null,
    val review: String? = null,
    @ColumnInfo(name = "created_date") val createdDate: String = DateTimeUtils.getCurrentDateTime()
)

enum class ActivityType {
    WATCHED_MOVIE,
    WATCHED_TV_SHOW,
    RATED_MOVIE,
    RATED_TV_SHOW,
    ADDED_TO_PLANNED,
    WROTE_REVIEW
}

// Enhanced search filters
data class SearchFilters(
    val query: String = "",
    val includeMovies: Boolean = true,
    val includeTvShows: Boolean = true,
    val minRating: Float = 0f,
    val maxRating: Float = 10f,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val genres: List<Int> = emptyList(),
    val sortBy: SortType = SortType.POPULARITY,
    val sortOrder: SortOrder = SortOrder.DESCENDING
)

enum class SortType {
    POPULARITY,
    RATING,
    RELEASE_DATE,
    TITLE
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

// Upcoming releases
@Entity(tableName = "upcoming_releases")
data class UpcomingRelease(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String,
    val isMovie: Boolean = true,
    val tmdbId: Int,
    val backdropPath: String? = null,
    @ColumnInfo(name = "is_interested") val isInterested: Boolean = false,
    @ColumnInfo(name = "notification_enabled") val notificationEnabled: Boolean = false
)

// User profile data
data class UserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val joinDate: String = DateTimeUtils.getCurrentDateTime(),
    val totalWatchedMovies: Int = 0,
    val totalWatchedTvShows: Int = 0,
    val totalWatchTime: Int = 0, // in minutes
    val averageRating: Float = 0f,
    val favoriteGenres: List<String> = emptyList(),
    val isPublic: Boolean = true
)

// Statistics model
data class DetailedStatistics(
    val totalWatchedMovies: Int = 0,
    val totalWatchedTvShows: Int = 0,
    val totalPlannedMovies: Int = 0,
    val totalPlannedTvShows: Int = 0,
    val totalWatchingMovies: Int = 0,
    val totalWatchingTvShows: Int = 0,
    val totalWatchTimeMinutes: Int = 0,
    val averageUserRating: Float = 0f,
    val topGenres: List<GenreStats> = emptyList(),
    val watchingStreak: Int = 0,
    val thisMonthWatched: Int = 0,
    val thisYearWatched: Int = 0
)

data class GenreStats(
    val genreId: Int,
    val genreName: String,
    val count: Int,
    val averageRating: Float
)
