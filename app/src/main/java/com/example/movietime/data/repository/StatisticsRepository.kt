package com.example.movietime.data.repository

import android.util.Log
import com.example.movietime.data.db.*
import com.example.movietime.data.model.Genre
import com.example.movietime.data.model.DetailedStatistics
import com.example.movietime.data.model.GenreStatItem
import com.example.movietime.data.model.DirectorStatItem
import com.example.movietime.data.model.TopRatedItem
import com.example.movietime.data.model.LongestItem
import com.example.movietime.data.model.RewatchedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for calculating and managing statistics.
 * Powers the Year in Review (Spotify Wrapped-style) feature.
 */
@Singleton
class StatisticsRepository @Inject constructor(
    private val watchedItemDao: WatchedItemDao,
    private val rewatchDao: RewatchDao,
    private val yearlyStatsDao: YearlyStatsDao,
    private val tvShowProgressDao: TvShowProgressDao
) {
    companion object {
        private const val TAG = "StatisticsRepository"
        
        // Genre name mappings
        private val GENRE_NAMES = mapOf(
            28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
            80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
            14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
            9648 to "Mystery", 10749 to "Romance", 878 to "Sci-Fi", 10770 to "TV Movie",
            53 to "Thriller", 10752 to "War", 37 to "Western",
            10759 to "Action & Adventure", 10762 to "Kids", 10763 to "News", 
            10764 to "Reality", 10765 to "Sci-Fi & Fantasy", 10766 to "Soap", 
            10767 to "Talk", 10768 to "War & Politics"
        )
        
        // Localized genre names (Russian)
        private val GENRE_NAMES_RU = mapOf(
            28 to "Боевик", 12 to "Приключения", 16 to "Мультфильм", 35 to "Комедия",
            80 to "Криминал", 99 to "Документальный", 18 to "Драма", 10751 to "Семейный",
            14 to "Фэнтези", 36 to "История", 27 to "Ужасы", 10402 to "Музыка",
            9648 to "Детектив", 10749 to "Мелодрама", 878 to "Фантастика", 10770 to "Телефильм",
            53 to "Триллер", 10752 to "Военный", 37 to "Вестерн",
            10759 to "Боевик", 10762 to "Детский", 10763 to "Новости", 
            10764 to "Реалити", 10765 to "Фантастика", 10766 to "Мыльная опера", 
            10767 to "Ток-шоу", 10768 to "Военный"
        )
    }

    /**
     * Calculate and store yearly statistics for Year in Review.
     * Should be called at year end or on-demand.
     */
    suspend fun calculateYearlyStats(year: Int): YearlyStats = withContext(Dispatchers.IO) {
        Log.d(TAG, "Calculating yearly stats for $year")
        
        val calendar = Calendar.getInstance()
        
        // Start of year
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfYear = calendar.timeInMillis
        
        // End of year
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfYear = calendar.timeInMillis
        
        // Get all watched items for the year
        val allWatched = watchedItemDao.getAllSync()
        val watchedInYear = allWatched.filter { item ->
            item.lastUpdated?.let { it in startOfYear..endOfYear } ?: false
        }
        
        // Get rewatches for the year
        val rewatchesInYear = rewatchDao.getRewatchesInPeriod(startOfYear, endOfYear)
        
        // Calculate totals
        val totalMovies = watchedInYear.count { it.mediaType == "movie" }
        val totalTvEpisodes = tvShowProgressDao.getWatchedEpisodesCount()
        
        // Calculate total watch time, multiplying by watchCount for rewatches
        var totalWatchTimeMinutes = 0L
        watchedInYear.forEach { item ->
            if (item.mediaType == "movie") {
                totalWatchTimeMinutes += (item.runtime ?: 0) * item.watchCount
            }
        }
        // Add TV episode watch time
        totalWatchTimeMinutes += rewatchesInYear.sumOf { (it.watchTimeMinutes ?: 0).toLong() }
        
        // Calculate monthly breakdown
        val monthlyBreakdown = calculateMonthlyBreakdown(year, watchedInYear, rewatchesInYear)
        
        // Find most rewatched item
        val mostRewatched = rewatchDao.getMostRewatched(startOfYear, endOfYear)
        
        // Find top rated item
        val topRated = watchedInYear
            .filter { it.userRating != null }
            .maxByOrNull { it.userRating ?: 0f }
        
        // Find longest movie
        val longestMovie = watchedInYear
            .filter { it.mediaType == "movie" && it.runtime != null }
            .maxByOrNull { it.runtime ?: 0 }

        // Calculate Genre stats
        val genreCounts = mutableMapOf<Int, Int>()
        watchedInYear.forEach { item ->
            item.genreIds?.split(",")?.forEach { idStr ->
                val id = idStr.trim().toIntOrNull()
                if (id != null) {
                    genreCounts[id] = genreCounts.getOrDefault(id, 0) + 1
                }
            }
        }
        val topGenreEntry = genreCounts.maxByOrNull { it.value }
        val topGenreId = topGenreEntry?.key
        val topGenreName = if (topGenreId != null) getGenreName(topGenreId) else null
        
        val stats = YearlyStats(
            year = year,
            totalMovies = totalMovies,
            totalTvEpisodes = totalTvEpisodes,
            totalWatchTimeMinutes = totalWatchTimeMinutes,
            topRatedItemId = topRated?.id,
            topRatedItemTitle = topRated?.title,
            topRatedItemRating = topRated?.userRating,
            mostRewatchedItemId = mostRewatched?.itemId,
            mostRewatchedItemTitle = mostRewatched?.title,
            mostRewatchedCount = mostRewatched?.count ?: 0,
            longestMovieId = longestMovie?.id,
            longestMovieTitle = longestMovie?.title,
            longestMovieRuntime = longestMovie?.runtime,
            monthlyBreakdown = monthlyBreakdown,
            favoriteGenreId = topGenreId,
            favoriteGenreName = topGenreName,
            uniqueGenresCount = genreCounts.size,
            calculatedAt = System.currentTimeMillis()
        )
        
        // Save stats
        yearlyStatsDao.insert(stats)
        Log.d(TAG, "Yearly stats calculated and saved: $stats")
        
        stats
    }
    
    private fun calculateMonthlyBreakdown(
        year: Int,
        watchedItems: List<WatchedItem>,
        rewatches: List<RewatchEntry>
    ): String {
        val monthlyCounts = IntArray(12) { 0 }
        val calendar = Calendar.getInstance()
        
        // Count from watched items
        watchedItems.forEach { item ->
            item.lastUpdated?.let { timestamp ->
                calendar.timeInMillis = timestamp
                if (calendar.get(Calendar.YEAR) == year) {
                    val month = calendar.get(Calendar.MONTH)
                    monthlyCounts[month]++
                }
            }
        }
        
        // Count from rewatches
        rewatches.forEach { rewatch ->
            calendar.timeInMillis = rewatch.watchedAt
            if (calendar.get(Calendar.YEAR) == year) {
                val month = calendar.get(Calendar.MONTH)
                monthlyCounts[month]++
            }
        }
        
        // Convert to JSON array
        val jsonArray = JSONArray()
        monthlyCounts.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }
    
    /**
     * Get or calculate stats for a specific year.
     */
    suspend fun getYearlyStats(year: Int): YearlyStats? {
        return yearlyStatsDao.getStatsForYear(year) ?: run {
            // If no stats exist, calculate them
            calculateYearlyStats(year)
        }
    }
    
    /**
     * Get all available years with stats.
     */
    suspend fun getAvailableYears(): List<Int> {
        return yearlyStatsDao.getAvailableYears()
    }

    /**
     * Get all yearly stats for trend charts (sorted desc by DAO default).
     */
    suspend fun getAllYearlyStats(): List<YearlyStats> {
        return yearlyStatsDao.getAllStatsSync()
    }
    
    /**
     * Get watch time for a specific month.
     */
    suspend fun getMonthlyWatchTime(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        
        calendar.set(year, month, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis
        
        return rewatchDao.getTotalWatchTime(startOfMonth, endOfMonth) ?: 0L
    }
    
    /**
     * Get total lifetime stats.
     */
    suspend fun getLifetimeStats(): LifetimeStats {
        val totalWatchTime = yearlyStatsDao.getTotalLifetimeWatchTime() ?: 0L
        val totalMovies = yearlyStatsDao.getTotalLifetimeMovies() ?: 0
        val totalEpisodes = yearlyStatsDao.getTotalLifetimeEpisodes() ?: 0
        
        return LifetimeStats(
            totalWatchTimeMinutes = totalWatchTime,
            totalMovies = totalMovies,
            totalTvEpisodes = totalEpisodes
        )
    }
    
    /**
     * Get rewatch count for an item.
     */
    suspend fun getRewatchCount(itemId: Int, mediaType: String): Int {
        return rewatchDao.getRewatchCount(itemId, mediaType)
    }
    
    /**
     * Add a rewatch entry.
     */
    suspend fun addRewatch(
        itemId: Int,
        mediaType: String,
        title: String?,
        posterPath: String?,
        runtime: Int?,
        userRating: Float? = null,
        notes: String? = null
    ) {
        val entry = RewatchEntry(
            itemId = itemId,
            mediaType = mediaType,
            title = title,
            posterPath = posterPath,
            watchedAt = System.currentTimeMillis(),
            userRating = userRating,
            notes = notes,
            watchTimeMinutes = runtime
        )
        rewatchDao.insert(entry)
        Log.d(TAG, "Added rewatch entry for $title")
    }
    
    /**
     * Get recent rewatches.
     */
    suspend fun getRecentRewatches(limit: Int = 10): List<RewatchEntry> {
        return rewatchDao.getRecentRewatches(limit)
    }

    private fun getGenreName(id: Int): String {
        return GENRE_NAMES[id] ?: "Unknown"
    }
    
    fun getGenreNameLocalized(id: Int, languageCode: String = "ru"): String {
        return if (languageCode.startsWith("ru")) {
            GENRE_NAMES_RU[id] ?: GENRE_NAMES[id] ?: "Unknown"
        } else {
            GENRE_NAMES[id] ?: "Unknown"
        }
    }
    
    /**
     * Calculate detailed statistics for the Statistics page.
     * Includes watch time, favorite genres, and directors.
     */
    suspend fun getDetailedStatistics(
        directorCache: Map<Int, DirectorStatItem> = emptyMap()
    ): DetailedStatistics = withContext(Dispatchers.IO) {
        Log.d(TAG, "Calculating detailed statistics")
        
        val allWatched = watchedItemDao.getAllSync()
        val movies = allWatched.filter { it.mediaType == "movie" }
        val tvShows = allWatched.filter { it.mediaType == "tv" }
        
        // Total watch time
        var totalWatchTime = 0L
        movies.forEach { movie ->
            totalWatchTime += (movie.runtime ?: 0) * movie.watchCount
        }
        tvShows.forEach { tv ->
            val episodeRuntime = tv.episodeRuntime ?: 45
            val episodes = tv.totalEpisodes ?: 1
            totalWatchTime += episodeRuntime.toLong() * episodes
        }
        
        // Average ratings
        val movieRatings = movies.mapNotNull { it.userRating }
        val tvRatings = tvShows.mapNotNull { it.userRating }
        val avgMovieRating = if (movieRatings.isNotEmpty()) movieRatings.average().toFloat() else 0f
        val avgTvRating = if (tvRatings.isNotEmpty()) tvRatings.average().toFloat() else 0f
        
        // Genre statistics
        val genreCounts = mutableMapOf<Int, MutableList<WatchedItem>>()
        allWatched.forEach { item ->
            item.genreIds?.split(",")?.forEach { idStr ->
                val id = idStr.trim().toIntOrNull()
                if (id != null) {
                    genreCounts.getOrPut(id) { mutableListOf() }.add(item)
                }
            }
        }
        
        val totalItems = allWatched.size.toFloat().coerceAtLeast(1f)
        val favoriteGenres = genreCounts.map { (genreId, items) ->
            val genreWatchTime = items.sumOf { item ->
                if (item.mediaType == "movie") {
                    (item.runtime ?: 0).toLong() * item.watchCount
                } else {
                    val episodeRuntime = item.episodeRuntime ?: 45
                    val episodes = item.totalEpisodes ?: 1
                    episodeRuntime.toLong() * episodes
                }
            }
            val avgRating = items.mapNotNull { it.userRating }.average().toFloat()
                .let { if (it.isNaN()) 0f else it }
            
            GenreStatItem(
                genreId = genreId,
                genreName = getGenreNameLocalized(genreId),
                count = items.size,
                totalWatchTimeMinutes = genreWatchTime,
                percentage = (items.size / totalItems) * 100f,
                averageRating = avgRating
            )
        }.sortedByDescending { it.count }.take(10)
        
        // Top rated movies
        val topRatedMovies = movies
            .filter { it.userRating != null && it.userRating!! > 0f }
            .sortedByDescending { it.userRating }
            .take(5)
            .map { TopRatedItem(it.id, it.title, it.posterPath, it.userRating!!, "movie") }
        
        // Top rated TV shows
        val topRatedTvShows = tvShows
            .filter { it.userRating != null && it.userRating!! > 0f }
            .sortedByDescending { it.userRating }
            .take(5)
            .map { TopRatedItem(it.id, it.title, it.posterPath, it.userRating!!, "tv") }
        
        // Longest movie
        val longestMovie = movies
            .filter { it.runtime != null && it.runtime!! > 0 }
            .maxByOrNull { it.runtime!! }
            ?.let { LongestItem(it.id, it.title, it.posterPath, it.runtime!!, "movie") }
        
        // Longest TV show (by total episodes)
        val longestTv = tvShows
            .filter { it.totalEpisodes != null && it.totalEpisodes!! > 0 }
            .maxByOrNull { (it.episodeRuntime ?: 45) * (it.totalEpisodes ?: 1) }
            ?.let { 
                val totalRuntime = (it.episodeRuntime ?: 45) * (it.totalEpisodes ?: 1)
                LongestItem(it.id, it.title, it.posterPath, totalRuntime, "tv") 
            }
        
        // Most rewatched
        val mostRewatched = allWatched
            .filter { it.watchCount > 1 }
            .maxByOrNull { it.watchCount }
            ?.let { RewatchedItem(it.id, it.title, it.posterPath, it.watchCount, it.mediaType) }
        
        // Movie vs TV ratio
        val movieVsTvRatio = if (allWatched.isNotEmpty()) {
            movies.size.toFloat() / allWatched.size.toFloat()
        } else 0.5f
        
        // Watched by year distribution
        val watchedByYear = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        allWatched.forEach { item ->
            item.lastUpdated?.let { timestamp ->
                calendar.timeInMillis = timestamp
                val year = calendar.get(Calendar.YEAR)
                watchedByYear[year] = watchedByYear.getOrDefault(year, 0) + 1
            }
        }
        
        // Release year distribution
        val releaseYearDist = mutableMapOf<Int, Int>()
        allWatched.forEach { item ->
            item.releaseDate?.take(4)?.toIntOrNull()?.let { year ->
                releaseYearDist[year] = releaseYearDist.getOrDefault(year, 0) + 1
            }
        }
        
        // Total TV episodes from progress table
        val totalTvEpisodes = tvShowProgressDao.getWatchedEpisodesCount()
        
        DetailedStatistics(
            totalWatchTimeMinutes = totalWatchTime,
            totalMovies = movies.size,
            totalTvShows = tvShows.size,
            totalTvEpisodes = totalTvEpisodes,
            averageMovieRating = avgMovieRating,
            averageTvRating = avgTvRating,
            favoriteGenres = favoriteGenres,
            favoriteDirectors = directorCache.values.toList()
                .sortedByDescending { it.moviesWatched }.take(10),
            watchedByYear = watchedByYear,
            topRatedMovies = topRatedMovies,
            topRatedTvShows = topRatedTvShows,
            longestMovieWatched = longestMovie,
            longestTvShow = longestTv,
            mostRewatchedItem = mostRewatched,
            movieVsTvRatio = movieVsTvRatio,
            releaseYearDistribution = releaseYearDist
        )
    }
    
    /**
     * Get all watched movie IDs for director fetching.
     */
    suspend fun getWatchedMovieIds(): List<Int> = withContext(Dispatchers.IO) {
        watchedItemDao.getAllSync()
            .filter { it.mediaType == "movie" }
            .map { it.id }
    }
}

data class LifetimeStats(
    val totalWatchTimeMinutes: Long,
    val totalMovies: Int,
    val totalTvEpisodes: Int
) {
    val totalWatchTimeHours: Double get() = totalWatchTimeMinutes / 60.0
    val totalWatchTimeDays: Double get() = totalWatchTimeHours / 24.0
}
