package com.example.movietime.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.db.PlannedItem
import com.example.movietime.data.db.PlannedDao
import com.example.movietime.data.db.WatchingItem
import com.example.movietime.data.db.WatchingDao
import com.example.movietime.data.db.SearchHistoryItem
import com.example.movietime.data.db.SearchHistoryDao
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.MoviesResponse
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.model.TvShowsResponse
import com.example.movietime.data.model.TvSeasonDetails
import com.example.movietime.data.model.TvEpisodeDetails
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: TmdbApi,
    private val dao: WatchedItemDao,
    private val plannedDao: PlannedDao,
    private val watchingDao: WatchingDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val apiKey: String
) {

    // --- API Methods ---
    suspend fun searchMultiLanguage(query: String): List<Any> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()

        // detect script (simple heuristic): if contains Cyrillic -> prefer Ukrainian/Russian first
        val isCyrillic = query.any { ch -> ch in '\u0400'..'\u04FF' }
        val languages = if (isCyrillic) listOf("uk-UA", "ru-RU", "en-US") else listOf("en-US", "uk-UA", "ru-RU")

        // Normalization: lowercase, remove punctuation and diacritics
        fun normalize(text: String?): String = text?.let {
            val lowered = it.lowercase()
            // remove diacritics
            val normalized = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            normalized.replace(Regex("[^\\p{L}0-9\\s]"), "").trim()
        } ?: ""

        // Simple Cyrillic->Latin transliteration for better cross-language matching / API queries
        fun transliterateCyrToLat(input: String): String {
            val map = mapOf(
                '\u0430' to "a",'\u0431' to "b",'\u0432' to "v",'\u0433' to "h",'\u0491' to "g",'\u0434' to "d",'\u0435' to "e",'\u0454' to "e",
                '\u0436' to "zh",'\u0437' to "z",'\u0438' to "y",'\u0456' to "i",'\u0457' to "i",'\u0439' to "i",'\u043a' to "k",'\u043b' to "l",
                '\u043c' to "m",'\u043d' to "n",'\u043e' to "o",'\u043f' to "p",'\u0440' to "r",'\u0441' to "s",'\u0442' to "t",'\u0443' to "u",
                '\u0444' to "f",'\u0445' to "kh",'\u0446' to "ts",'\u0447' to "ch",'\u0448' to "sh",'\u0449' to "shch",'\u044c' to "",'\u044e' to "yu",'\u044f' to "ya"
            )
            val sb = StringBuilder()
            for (ch in input.lowercase()) {
                val repl = map[ch]
                if (repl != null) sb.append(repl) else sb.append(ch)
            }
            return sb.toString()
        }

        val qNorm = normalize(query)
        val qTrans = transliterateCyrToLat(query)

        // Build query variants to send to TMDB (original + transliteration if different)
        val queryVariants = mutableListOf<String>()
        queryVariants.add(query)
        if (qTrans.isNotBlank() && qTrans != qNorm && qTrans.lowercase() != query.lowercase()) {
            queryVariants.add(qTrans)
        }

        Log.d("AppRepo", "searchMultiLanguage() query='$query' qNorm='$qNorm' qTrans='$qTrans' langs=${languages.joinToString()} variants=${queryVariants.joinToString()}")

        val movieDeferred = mutableListOf<kotlinx.coroutines.Deferred<MoviesResponse>>()
        val tvDeferred = mutableListOf<kotlinx.coroutines.Deferred<TvShowsResponse>>()

        // For each language and each query variant, launch async requests
        for (language in languages) {
            for (q in queryVariants) {
                Log.d("AppRepo", "Launching request: type=movie lang=$language q=$q")
                movieDeferred.add(async {
                    try {
                        val resp = api.searchMovies(apiKey, q, language)
                        Log.d("AppRepo", "Movie response: lang=$language q=$q size=${resp.results.size}")
                        resp
                    } catch (e: Exception) {
                        Log.d("AppRepo", "Movie request failed: lang=$language q=$q error=${e.message}")
                        MoviesResponse(emptyList())
                    }
                })

                Log.d("AppRepo", "Launching request: type=tv lang=$language q=$q")
                tvDeferred.add(async {
                    try {
                        val resp = api.searchTvShows(apiKey, q, language)
                        Log.d("AppRepo", "TV response: lang=$language q=$q size=${resp.results.size}")
                        resp
                    } catch (e: Exception) {
                        Log.d("AppRepo", "TV request failed: lang=$language q=$q error=${e.message}")
                        TvShowsResponse(emptyList())
                    }
                })
            }
        }

        val movieResponses = movieDeferred.awaitAll()
        val tvResponses = tvDeferred.awaitAll()

        val movieItems = movieResponses.flatMap { it.results }
        val tvItems = tvResponses.flatMap { it.results }

        Log.d("AppRepo", "Total items fetched: movies=${movieItems.size} tv=${tvItems.size}")

        // Combine items and deduplicate by (id, mediaType)
        val allItems = (movieItems + tvItems)
        val uniqueMap = linkedMapOf<Pair<Int, String>, MutableList<Any>>()
        allItems.forEach { item ->
            val key = when (item) {
                is MovieResult -> item.id to "movie"
                is TvShowResult -> item.id to "tv"
                else -> 0 to ""
            }
            uniqueMap.getOrPut(key) { mutableListOf() }.add(item)
        }

        // For each unique item, compute a relevance score based on query matches across available titles

        // Levenshtein distance for lightweight fuzzy scoring
        fun levenshtein(a: String, b: String): Int {
            if (a == b) return 0
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                }
            }
            return dp[a.length][b.length]
        }

        data class Scored(val item: Any, val score: Int, val vote: Int)

        val scored = uniqueMap.map { (_, occurrences) ->
            // occurrences contains same item from different language results; pick first as representative
            val item = occurrences.first()
            val titles = when (item) {
                is MovieResult -> listOf(item.title)
                is TvShowResult -> listOf(item.name)
                else -> listOf(null)
            }.map { normalize(it) }

            var score = 0
            var vote = occurrences.size

            // exact match > startsWith > contains
            for (t in titles) {
                if (t.isBlank()) continue
                // exact or transliterated exact match
                if (t == qNorm || t == qTrans) {
                    score += 200
                } else if (t.startsWith(qNorm)) {
                    score += 120
                } else if (t.contains(qNorm)) {
                    score += 60
                } else if (qTrans.isNotBlank() && t.contains(qTrans)) {
                    score += 80
                } else {
                    // small fuzzy match: common prefix length
                    val common = maxOf(t.commonPrefixWith(qNorm).length, t.commonPrefixWith(qTrans).length)
                    score += common * 2
                    // also use levenshtein distance to give small score for close matches
                    val dist = levenshtein(t, qNorm).coerceAtMost(levenshtein(t, qTrans))
                    if (dist <= 3) score += (4 - dist) * 15
                }
            }

            // boost by popularity if available
            val popularityBoost = when (item) {
                is MovieResult -> (item.voteAverage * 10).toInt()
                is TvShowResult -> (item.voteAverage * 10).toInt()
                else -> 0
            }
            score += popularityBoost

            Scored(item, score, vote)
        }

        // Sort by score desc, then by vote (appears in multiple languages), then by id
        val sorted = scored.sortedWith(compareByDescending<Scored> { it.score }
            .thenByDescending { it.vote })

        // Return top 100 items as List<Any>
        val result = sorted.map { it.item }.take(100)
        Log.d("AppRepo", "Returning results count=${result.size}")
        result
    }

    // Details endpoints return single objects
    suspend fun getMovieDetails(movieId: Int, language: String = "en-US"): MovieResult {
        Log.d("AppRepo", "getMovieDetails: movieId=$movieId, apiKey=${apiKey.take(10)}..., language=$language")
        try {
            val result = api.getMovieDetails(movieId, apiKey, language)
            Log.d("AppRepo", "getMovieDetails success: title=${result.title}, runtime=${result.runtime}")
            return result
        } catch (e: retrofit2.HttpException) {
            Log.e("AppRepo", "HTTP Error ${e.code()}: ${e.message()}", e)
            throw e
        } catch (e: java.net.UnknownHostException) {
            Log.e("AppRepo", "Network Error: Cannot resolve host api.themoviedb.org - check internet connection", e)
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AppRepo", "Network Error: Connection timeout - check internet connection", e)
            throw e
        } catch (e: Exception) {
            Log.e("AppRepo", "getMovieDetails failed: ${e.javaClass.simpleName} - ${e.message}", e)
            throw e
        }
    }

    suspend fun getTvShowDetails(tvId: Int, language: String = "en-US"): TvShowResult {
        Log.d("AppRepo", "getTvShowDetails: tvId=$tvId, apiKey=${apiKey.take(10)}..., language=$language")
        try {
            val result = api.getTvShowDetails(tvId, apiKey, language)
            Log.d("AppRepo", "getTvShowDetails success: name=${result.name}")
            return result
        } catch (e: retrofit2.HttpException) {
            Log.e("AppRepo", "HTTP Error ${e.code()}: ${e.message()}", e)
            throw e
        } catch (e: java.net.UnknownHostException) {
            Log.e("AppRepo", "Network Error: Cannot resolve host api.themoviedb.org - check internet connection", e)
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AppRepo", "Network Error: Connection timeout - check internet connection", e)
            throw e
        } catch (e: Exception) {
            Log.e("AppRepo", "getTvShowDetails failed: ${e.javaClass.simpleName} - ${e.message}", e)
            throw e
        }
    }

    suspend fun getPopularMovies(language: String = "en-US"): MoviesResponse {
        return api.getPopularMovies(apiKey, language)
    }

    suspend fun getPopularTvShows(language: String = "en-US"): TvShowsResponse {
        return api.getPopularTvShows(apiKey, language)
    }

    fun getWatchedItems(): LiveData<List<WatchedItem>> {
        return dao.getAll()
    }

    suspend fun getWatchedItemsSync(): List<WatchedItem> {
        return dao.getAllSync()
    }

    suspend fun getWatchedItemsCount(): Int {
        return dao.getCount()
    }

    suspend fun getAllWatchedSync(): List<WatchedItem> {
        return dao.getAllSync()
    }

    /**
     * Отримує детальну інформацію про сезон з усіма епізодами та їх тривалістю
     */
    suspend fun getSeasonDetails(tvId: Int, seasonNumber: Int, language: String = "uk-UA"): TvSeasonDetails {
        Log.d("AppRepo", "getSeasonDetails: tvId=$tvId, seasonNumber=$seasonNumber, language=$language")
        Log.d("AppRepo", "API Key length: ${apiKey.length} (${apiKey.take(8)}...)")

        return try {
            Log.d("AppRepo", "Making API request to TMDB...")
            val result = api.getSeasonDetails(tvId, seasonNumber, apiKey, language)
            Log.d("AppRepo", "getSeasonDetails success:")
            Log.d("AppRepo", "- Season name: ${result.name}")
            Log.d("AppRepo", "- Episodes count: ${result.episodes?.size}")
            Log.d("AppRepo", "- Season number: ${result.seasonNumber}")

            result.episodes?.forEachIndexed { index, episode ->
                if (index < 3) { // Log only first 3 episodes to avoid spam
                    Log.d("AppRepo", "  Episode ${episode.episodeNumber}: ${episode.name}, runtime=${episode.runtime}")
                }
            }

            result
        } catch (e: retrofit2.HttpException) {
            Log.e("AppRepo", "HTTP Error ${e.code()}: ${e.message()}")
            Log.e("AppRepo", "Response: ${e.response()?.errorBody()?.string()}")

            // Fallback to English if Ukrainian fails and it's a 404
            if (language != "en-US" && e.code() == 404) {
                Log.d("AppRepo", "Retrying with English due to 404...")
                try {
                    api.getSeasonDetails(tvId, seasonNumber, apiKey, "en-US")
                } catch (enE: Exception) {
                    Log.e("AppRepo", "English fallback also failed: ${enE.message}")
                    throw enE
                }
            } else {
                throw e
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AppRepo", "Timeout getting season details", e)
            throw Exception("Таймаут API: перевірте інтернет з'єднання")
        } catch (e: java.net.UnknownHostException) {
            Log.e("AppRepo", "Network error getting season details", e)
            throw Exception("Немає інтернет з'єднання")
        } catch (e: Exception) {
            Log.e("AppRepo", "getSeasonDetails failed: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Отримує детальну інформацію про всі сезони серіалу з епізодами
     */
    suspend fun getAllSeasonsDetails(tvId: Int, totalSeasons: Int, language: String = "uk-UA"): List<TvSeasonDetails> {
        Log.d("AppRepo", "getAllSeasonsDetails: tvId=$tvId, totalSeasons=$totalSeasons")
        val seasons = mutableListOf<TvSeasonDetails>()

        for (seasonNumber in 1..totalSeasons) {
            try {
                val seasonDetails = getSeasonDetails(tvId, seasonNumber, language)
                seasons.add(seasonDetails)
                Log.d("AppRepo", "Loaded season $seasonNumber with ${seasonDetails.episodes?.size} episodes")
            } catch (e: Exception) {
                Log.e("AppRepo", "Failed to load season $seasonNumber: ${e.message}")
                // Continue loading other seasons even if one fails
            }
        }

        Log.d("AppRepo", "getAllSeasonsDetails completed: loaded ${seasons.size} seasons")
        return seasons
    }

    suspend fun addWatchedItem(item: WatchedItem) {
        Log.d("AppRepository", "Inserting watched item: id=${item.id}, title=${item.title}, mediaType=${item.mediaType}, runtime=${item.runtime}")
        try {
            dao.insert(item)
            Log.d("AppRepository", "Successfully inserted watched item: ${item.id}")
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to insert watched item: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteWatchedItem(item: WatchedItem) {
        dao.deleteById(item.id, item.mediaType)
    }

    // Updated: get watched item by id + mediaType (composite primary key)
    suspend fun getWatchedItemById(id: Int, mediaType: String): WatchedItem? {
        return dao.getById(id, mediaType)
    }

    // --- Planned Content Methods ---
    @Suppress("unused")
    fun getPlannedContent(): LiveData<List<PlannedItem>> {
        return plannedDao.getAll()
    }

    suspend fun getPlannedContentSync(): List<WatchedItem> {
        return plannedDao.getAllSync().map { plannedItem ->
            WatchedItem(
                id = plannedItem.id,
                title = plannedItem.title,
                posterPath = plannedItem.posterPath,
                releaseDate = plannedItem.releaseDate,
                runtime = plannedItem.runtime,
                mediaType = plannedItem.mediaType
            )
        }
    }

    suspend fun addToPlanned(item: WatchedItem) {
        val plannedItem = PlannedItem(
            id = item.id,
            title = item.title,
            posterPath = item.posterPath,
            releaseDate = item.releaseDate,
            runtime = item.runtime,
            mediaType = item.mediaType
        )
        plannedDao.insert(plannedItem)
    }

    suspend fun removeFromPlanned(id: Int, mediaType: String) {
        plannedDao.deleteById(id, mediaType)
    }

    suspend fun getPlannedItemById(id: Int, mediaType: String): PlannedItem? {
        return plannedDao.getById(id, mediaType)
    }

    suspend fun isItemPlanned(id: Int, mediaType: String): Boolean {
        return getPlannedItemById(id, mediaType) != null
    }

    @Suppress("unused")
    suspend fun getPlannedMoviesCount(): Int {
        return plannedDao.getCountByMediaType("movie")
    }

    @Suppress("unused")
    suspend fun getPlannedTvShowsCount(): Int {
        return plannedDao.getCountByMediaType("tv")
    }

    @Suppress("unused")
    suspend fun getTotalPlannedCount(): Int {
        return plannedDao.getCount()
    }

    // Method to check if an item exists in any list (watched or planned)
    @Suppress("unused")
    suspend fun isItemExists(id: Int, mediaType: String): Pair<Boolean, Boolean> {
        val isWatched = getWatchedItemById(id, mediaType) != null
        val isPlanned = isItemPlanned(id, mediaType)
        return Pair(isWatched, isPlanned)
    }

    // --- Watching Content Methods ---
    @Suppress("unused")
    fun getWatchingContent(): LiveData<List<WatchingItem>> {
        return watchingDao.getAll()
    }

    suspend fun getWatchingContentSync(): List<WatchedItem> {
        return watchingDao.getAllSync().map { watchingItem ->
            WatchedItem(
                id = watchingItem.id,
                title = watchingItem.title,
                posterPath = watchingItem.posterPath,
                releaseDate = watchingItem.releaseDate,
                runtime = watchingItem.runtime,
                mediaType = watchingItem.mediaType
            )
        }
    }

    suspend fun addToWatching(item: WatchedItem) {
        val watchingItem = WatchingItem(
            id = item.id,
            title = item.title,
            posterPath = item.posterPath,
            releaseDate = item.releaseDate,
            runtime = item.runtime,
            mediaType = item.mediaType
        )
        watchingDao.insert(watchingItem)
    }

    suspend fun removeFromWatching(id: Int, mediaType: String) {
        watchingDao.deleteById(id, mediaType)
    }

    suspend fun getWatchingItemById(id: Int, mediaType: String): WatchingItem? {
        return watchingDao.getById(id, mediaType)
    }

    @Suppress("unused")
    suspend fun isItemWatching(id: Int, mediaType: String): Boolean {
        return getWatchingItemById(id, mediaType) != null
    }

    @Suppress("unused")
    suspend fun getWatchingMoviesCount(): Int {
        return watchingDao.getCountByMediaType("movie")
    }

    @Suppress("unused")
    suspend fun getWatchingTvShowsCount(): Int {
        return watchingDao.getCountByMediaType("tv")
    }

    @Suppress("unused")
    suspend fun getTotalWatchingCount(): Int {
        return watchingDao.getCount()
    }

    suspend fun getRecentActivity(limit: Int = 10): List<com.example.movietime.data.model.RecentActivityItem> = coroutineScope {
        android.util.Log.d("AppRepository", "getRecentActivity: Starting...")
        
        // Get search history - items user searched for and clicked on
        val searchHistory = searchHistoryDao.getRecent(limit)
        
        android.util.Log.d("AppRepository", "getRecentActivity: searchHistory=${searchHistory.size}")

        val allItems = mutableListOf<com.example.movietime.data.model.RecentActivityItem>()

        // Add search history items as "Searched" type
        allItems.addAll(searchHistory.map { 
            com.example.movietime.data.model.RecentActivityItem.Searched(
                id = it.id, 
                title = it.title, 
                timestamp = it.timestamp, 
                mediaType = it.mediaType,
                posterPath = it.posterPath,
                voteAverage = it.voteAverage
            ) 
        })

        android.util.Log.d("AppRepository", "getRecentActivity: total items=${allItems.size}")
        
        // Sort by timestamp desc to show actual most recent items at the top
        val result = allItems.sortedByDescending { it.timestamp }.take(limit)
        android.util.Log.d("AppRepository", "getRecentActivity: returning ${result.size} items")
        result
    }

    // --- Search History Methods ---
    
    suspend fun addToSearchHistory(item: SearchHistoryItem) {
        searchHistoryDao.insert(item)
    }
    
    suspend fun addMovieToSearchHistory(movie: MovieResult) {
        val historyItem = SearchHistoryItem(
            id = movie.id,
            title = movie.title ?: "Unknown",
            posterPath = movie.posterPath,
            mediaType = "movie",
            releaseDate = movie.releaseDate,
            voteAverage = movie.voteAverage.toDouble()
        )
        searchHistoryDao.insert(historyItem)
    }
    
    suspend fun addTvShowToSearchHistory(tvShow: TvShowResult) {
        val historyItem = SearchHistoryItem(
            id = tvShow.id,
            title = tvShow.name ?: "Unknown",
            posterPath = tvShow.posterPath,
            mediaType = "tv",
            releaseDate = tvShow.firstAirDate,
            voteAverage = tvShow.voteAverage.toDouble()
        )
        searchHistoryDao.insert(historyItem)
    }
    
    suspend fun clearSearchHistory() {
        searchHistoryDao.deleteAll()
    }
    
    suspend fun removeFromSearchHistory(id: Int, mediaType: String) {
        searchHistoryDao.delete(id, mediaType)
    }

    // --- Recommendations & Similar Content ---

    suspend fun getMovieRecommendations(movieId: Int, language: String = "uk-UA"): List<MovieResult> {
        return try {
            val response = api.getMovieRecommendations(movieId, apiKey, language)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTvShowRecommendations(tvId: Int, language: String = "uk-UA"): List<TvShowResult> {
        return try {
            val response = api.getTvShowRecommendations(tvId, apiKey, language)
            response.results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUpcomingMovies(): List<MovieResult> {
        return try {
            val response = api.getUpcomingMovies(apiKey, "uk-UA", "UA")
            response.results
        } catch (e: Exception) {
            Log.e("AppRepo", "Failed to get upcoming movies: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUpcomingTvShows(): List<TvShowResult> {
        return try {
            val response = api.getOnTheAirTvShows(apiKey, "uk-UA")
            response.results
        } catch (e: Exception) {
            Log.e("AppRepo", "Failed to get on-the-air TV shows: ${e.message}")
            emptyList()
        }
    }
}