package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user's watch statistics for Year in Review feature.
 * Aggregated data that's calculated periodically.
 */
@Entity(tableName = "yearly_stats")
data class YearlyStats(
    @PrimaryKey
    val year: Int,
    val totalMovies: Int = 0,
    val totalTvEpisodes: Int = 0,
    val totalWatchTimeMinutes: Long = 0,
    val favoriteGenreId: Int? = null,
    val favoriteGenreName: String? = null,
    val favoriteActorId: Int? = null,
    val favoriteActorName: String? = null,
    val favoriteDirectorId: Int? = null,
    val favoriteDirectorName: String? = null,
    val topRatedItemId: Int? = null,
    val topRatedItemTitle: String? = null,
    val topRatedItemRating: Float? = null,
    val mostRewatchedItemId: Int? = null,
    val mostRewatchedItemTitle: String? = null,
    val mostRewatchedCount: Int = 0,
    val longestMovieId: Int? = null,
    val longestMovieTitle: String? = null,
    val longestMovieRuntime: Int? = null,
    val bingeWatchedSeriesId: Int? = null,
    val bingeWatchedSeriesTitle: String? = null,
    val uniqueGenresCount: Int = 0,
    val uniqueActorsCount: Int = 0,
    val uniqueDirectorsCount: Int = 0,
    val monthlyBreakdown: String? = null, // JSON array of 12 monthly counts
    val calculatedAt: Long = System.currentTimeMillis()
)
