package com.example.movietime.data.repository

import android.content.Context
import android.util.Log
import com.example.movietime.R
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.TvShowProgress
import com.example.movietime.data.db.TvShowProgressDao
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.db.WatchingDao
import com.example.movietime.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.example.movietime.BuildConfig

/**
 * Repository for the "Today" digest screen.
 * Aggregates data from multiple sources for the daily digest.
 */
@Singleton
class TodayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tmdbApi: TmdbApi,
    private val watchedItemDao: WatchedItemDao,
    private val watchingDao: WatchingDao,
    private val tvShowProgressDao: TvShowProgressDao
) {
    private val apiKey = BuildConfig.TMDB_API_KEY
    
    companion object {
        private const val TAG = "TodayRepository"
    }

    /**
     * Get the complete Today digest
     */
    suspend fun getTodayDigest(): TodayDigest = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val dateString = dateFormat.format(today.time)
        
        // Get greeting based on time of day
        val greeting = getGreeting(today.get(Calendar.HOUR_OF_DAY))
        
        // Load data in parallel
        val newEpisodesDeferred = async { getNewEpisodes() }
        val todayReleasesDeferred = async { getTodayReleases() }
        val upcomingDeferred = async { getUpcomingThisWeek() }
        val continueWatchingDeferred = async { getContinueWatching() }
        val tipsDeferred = async { getPersonalTips() }
        
        TodayDigest(
            greeting = greeting,
            date = dateString,
            newEpisodes = newEpisodesDeferred.await(),
            todayReleases = todayReleasesDeferred.await(),
            upcomingThisWeek = upcomingDeferred.await(),
            trendingNews = getTrendingNews(),
            personalTips = tipsDeferred.await(),
            continueWatching = continueWatchingDeferred.await()
        )
    }
    
    private fun getGreeting(hour: Int): String {
        return when (hour) {
            in 5..11 -> context.getString(R.string.good_morning)
            in 12..17 -> context.getString(R.string.good_afternoon)
            in 18..22 -> context.getString(R.string.good_evening)
            else -> context.getString(R.string.good_night)
        }
    }
    
    /**
     * Get new episodes for TV shows the user is watching
     */
    private suspend fun getNewEpisodes(): List<NewEpisodeItem> {
        val episodes = mutableListOf<NewEpisodeItem>()
        
        try {
            // Get all TV shows user is watching
            val watchingTvShows = watchingDao.getAllSync()
                .filter { it.mediaType == "tv" }
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7) // Last 7 days
            val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            
            // Check each show for new episodes
            for (show in watchingTvShows.take(20)) { // Limit API calls
                try {
                    val details = tmdbApi.getTvShowDetails(
                        tvId = show.id,
                        apiKey = apiKey,
                        language = getLanguage()
                    )
                    
                    // Get the latest season details
                    val latestSeason = details.numberOfSeasons ?: 0
                    if (latestSeason > 0) {
                        val seasonDetails = tmdbApi.getSeasonDetails(
                            tvId = show.id,
                            seasonNumber = latestSeason,
                            apiKey = apiKey,
                            language = getLanguage()
                        )
                        
                        // Find episodes that aired recently
                        seasonDetails.episodes?.forEach { episode ->
                            val airDate = episode.airDate
                            if (airDate != null && airDate >= weekAgo && airDate <= today) {
                                // Check if user already watched this episode
                                val episodeNum = episode.episodeNumber ?: 0
                                val isWatched = tvShowProgressDao.isEpisodeWatched(
                                    show.id, 
                                    latestSeason, 
                                    episodeNum
                                ) ?: false
                                
                                episodes.add(
                                    NewEpisodeItem(
                                        tvShowId = show.id,
                                        tvShowName = show.title,
                                        posterPath = show.posterPath,
                                        backdropPath = details.backdropPath,
                                        seasonNumber = latestSeason,
                                        episodeNumber = episodeNum,
                                        episodeName = episode.name,
                                        airDate = airDate,
                                        overview = episode.overview,
                                        runtime = episode.runtime,
                                        voteAverage = episode.voteAverage ?: 0f,
                                        isWatched = isWatched
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching episodes for ${show.title}: ${e.message}")
                }
            }
            
            // Sort by air date (newest first) and limit
            episodes.sortByDescending { it.airDate }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting new episodes: ${e.message}")
        }
        
        return episodes.take(10)
    }
    
    /**
     * Get movies and TV shows releasing today
     */
    private suspend fun getTodayReleases(): List<TodayReleaseItem> {
        val releases = mutableListOf<TodayReleaseItem>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        try {
            // Get upcoming movies
            val upcomingMovies = tmdbApi.getUpcomingMovies(
                apiKey = apiKey,
                language = getLanguage(),
                region = "UA"
            )
            
            upcomingMovies.results
                .filter { it.releaseDate == today }
                .forEach { movie ->
                    releases.add(
                        TodayReleaseItem(
                            id = movie.id,
                            title = movie.title ?: "",
                            posterPath = movie.posterPath,
                            backdropPath = movie.backdropPath,
                            releaseDate = movie.releaseDate,
                            mediaType = "movie",
                            voteAverage = movie.voteAverage ?: 0f,
                            overview = movie.overview,
                            genres = emptyList(),
                            isInWatchlist = false
                        )
                    )
                }
            
            // Get TV shows airing today
            val airingToday = tmdbApi.getOnTheAirTvShows(
                apiKey = apiKey,
                language = getLanguage()
            )
            
            airingToday.results
                .filter { it.firstAirDate == today }
                .forEach { show ->
                    releases.add(
                        TodayReleaseItem(
                            id = show.id,
                            title = show.name ?: "",
                            posterPath = show.posterPath,
                            backdropPath = show.backdropPath,
                            releaseDate = show.firstAirDate,
                            mediaType = "tv",
                            voteAverage = show.voteAverage ?: 0f,
                            overview = show.overview,
                            genres = emptyList(),
                            isInWatchlist = false
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today releases: ${e.message}")
        }
        
        return releases.sortedByDescending { it.voteAverage }.take(10)
    }
    
    /**
     * Get upcoming releases for this week
     */
    private suspend fun getUpcomingThisWeek(): List<TodayReleaseItem> {
        val releases = mutableListOf<TodayReleaseItem>()
        
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val nextWeek = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        
        try {
            // Get upcoming movies for this week
            val upcomingMovies = tmdbApi.getUpcomingMovies(
                apiKey = apiKey,
                language = getLanguage(),
                region = "UA"
            )
            
            upcomingMovies.results
                .filter { 
                    val date = it.releaseDate
                    date != null && date > today && date <= nextWeek 
                }
                .forEach { movie ->
                    releases.add(
                        TodayReleaseItem(
                            id = movie.id,
                            title = movie.title ?: "",
                            posterPath = movie.posterPath,
                            backdropPath = movie.backdropPath,
                            releaseDate = movie.releaseDate,
                            mediaType = "movie",
                            voteAverage = movie.voteAverage ?: 0f,
                            overview = movie.overview,
                            genres = emptyList(),
                            isInWatchlist = false
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting upcoming releases: ${e.message}")
        }
        
        return releases.sortedBy { it.releaseDate }.take(15)
    }
    
    /**
     * Get shows/movies user should continue watching
     */
    private suspend fun getContinueWatching(): List<ContinueWatchingItem> {
        val items = mutableListOf<ContinueWatchingItem>()
        
        try {
            // Get TV shows with progress
            val watchingShows = watchingDao.getAllSync()
                .filter { it.mediaType == "tv" }
            
            for (show in watchingShows.take(10)) {
                val progress = tvShowProgressDao.getProgressForShowSync(show.id)
                if (progress.isNotEmpty()) {
                    val totalEpisodes = progress.size
                    val watchedEpisodes = progress.count { it.watched }
                    
                    if (watchedEpisodes < totalEpisodes) {
                        // Find the next unwatched episode
                        val nextUnwatched = progress
                            .filter { !it.watched }
                            .minByOrNull { it.seasonNumber * 1000 + it.episodeNumber }
                        
                        val lastWatched = progress
                            .filter { it.watched }
                            .maxByOrNull { it.seasonNumber * 1000 + it.episodeNumber }
                        
                        items.add(
                            ContinueWatchingItem(
                                id = show.id,
                                title = show.title,
                                posterPath = show.posterPath,
                                backdropPath = null, // WatchingItem doesn't have backdropPath
                                mediaType = "tv",
                                progress = watchedEpisodes.toFloat() / totalEpisodes,
                                lastWatchedEpisode = lastWatched?.let { "S${it.seasonNumber}E${it.episodeNumber}" },
                                nextEpisode = nextUnwatched?.let { "S${it.seasonNumber}E${it.episodeNumber}" },
                                totalEpisodes = totalEpisodes,
                                watchedEpisodes = watchedEpisodes
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting continue watching: ${e.message}")
        }
        
        return items.sortedByDescending { it.progress }.take(5)
    }
    
    /**
     * Generate personal tips based on user activity
     */
    private suspend fun getPersonalTips(): List<PersonalTip> {
        val tips = mutableListOf<PersonalTip>()
        
        try {
            // Tip: Continue watching if there's progress
            val continueWatching = getContinueWatching()
            if (continueWatching.isNotEmpty()) {
                val show = continueWatching.first()
                tips.add(
                    PersonalTip(
                        id = "continue_${show.id}",
                        type = TipType.CONTINUE_WATCHING,
                        title = context.getString(R.string.tip_continue_watching),
                        description = context.getString(R.string.tip_continue_watching_desc, show.title, show.nextEpisode ?: ""),
                        actionText = context.getString(R.string.continue_watching),
                        relatedItemId = show.id,
                        relatedItemType = "tv"
                    )
                )
            }
            
            // Tip: Check watched count for milestone
            val watchedCount = watchedItemDao.getCount()
            if (watchedCount > 0 && watchedCount % 10 == 0) {
                tips.add(
                    PersonalTip(
                        id = "milestone_$watchedCount",
                        type = TipType.MILESTONE_REACHED,
                        title = context.getString(R.string.tip_milestone_title),
                        description = context.getString(R.string.tip_milestone_desc, watchedCount),
                        actionText = context.getString(R.string.view_statistics)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating tips: ${e.message}")
        }
        
        return tips.take(3)
    }
    
    /**
     * Get trending news (placeholder - would need external news API)
     */
    private fun getTrendingNews(): List<NewsItem> {
        // This would typically come from a movie news API
        // For now, return empty - could be implemented with RSS feeds or news APIs
        return emptyList()
    }
    
    private fun getLanguage(): String {
        return when (Locale.getDefault().language) {
            "uk" -> "uk-UA"
            "ru" -> "ru-RU"
            else -> "en-US"
        }
    }
}
