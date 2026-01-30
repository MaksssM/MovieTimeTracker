package com.example.movietime.data.model

/**
 * Comprehensive statistics data for the Statistics page.
 * Contains total watch time, favorite genres, directors, and more.
 */
data class DetailedStatistics(
    // Time stats
    val totalWatchTimeMinutes: Long = 0,
    val totalMovies: Int = 0,
    val totalTvShows: Int = 0,
    val totalTvEpisodes: Int = 0,
    
    // Average ratings
    val averageMovieRating: Float = 0f,
    val averageTvRating: Float = 0f,
    
    // Favorite genres (sorted by count)
    val favoriteGenres: List<GenreStatItem> = emptyList(),
    
    // Favorite directors (sorted by count)
    val favoriteDirectors: List<DirectorStatItem> = emptyList(),
    
    // Watch patterns
    val watchedByYear: Map<Int, Int> = emptyMap(),
    val watchedByMonth: Map<Int, Int> = emptyMap(),
    
    // Top rated
    val topRatedMovies: List<TopRatedItem> = emptyList(),
    val topRatedTvShows: List<TopRatedItem> = emptyList(),
    
    // Streaks and milestones
    val longestMovieWatched: LongestItem? = null,
    val longestTvShow: LongestItem? = null,
    val mostRewatchedItem: RewatchedItem? = null,
    
    // Content breakdown
    val movieVsTvRatio: Float = 0.5f, // 0.0 = all TV, 1.0 = all movies
    
    // Release year distribution
    val releaseYearDistribution: Map<Int, Int> = emptyMap(),

    // This month stats
    val thisMonthMovies: Int = 0,
    val thisMonthEpisodes: Int = 0,
    val thisMonthMinutes: Long = 0,

    // Achievements
    val currentStreak: Int = 0,
    val bestMonth: BestMonthItem? = null,
    
    // Enhanced statistics
    val avgDailyWatchMinutes: Long = 0,
    val totalUniqueGenres: Int = 0,
    val firstWatchDate: Long? = null,
    val completedTvShows: Int = 0,
    val avgEpisodesPerDay: Float = 0f,
    val decadeDistribution: Map<String, Int> = emptyMap()
)

/**
 * Genre statistics item
 */
data class GenreStatItem(
    val genreId: Int,
    val genreName: String,
    val count: Int,
    val totalWatchTimeMinutes: Long = 0,
    val percentage: Float = 0f, // Percentage of total
    val averageRating: Float = 0f
)

/**
 * Director statistics item
 */
data class DirectorStatItem(
    val directorId: Int,
    val directorName: String,
    val profilePath: String? = null,
    val moviesWatched: Int = 0,
    val totalWatchTimeMinutes: Long = 0,
    val averageRating: Float = 0f,
    val movieTitles: List<String> = emptyList()
)

/**
 * Top rated item for display
 */
data class TopRatedItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val userRating: Float,
    val mediaType: String
)

/**
 * Longest watched item
 */
data class LongestItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val runtimeMinutes: Int,
    val mediaType: String
)

/**
 * Most rewatched item
 */
data class RewatchedItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val rewatchCount: Int,
    val mediaType: String
)

/**
 * Best month statistics
 */
data class BestMonthItem(
    val monthName: String,
    val year: Int,
    val count: Int,
    val watchTimeMinutes: Long = 0
)

/**
 * Director info from TMDB credits
 */
data class DirectorInfo(
    val id: Int,
    val name: String,
    val profilePath: String?
)
