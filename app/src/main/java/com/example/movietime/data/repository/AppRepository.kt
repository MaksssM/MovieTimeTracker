package com.example.movietime.data.repository

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.LiveData
import com.example.movietime.BuildConfig
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
import com.example.movietime.data.model.CompanyResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton

import com.example.movietime.util.LanguageManager

@Singleton
class AppRepository @Inject constructor(
    private val api: TmdbApi,
    private val dao: WatchedItemDao,
    private val plannedDao: PlannedDao,
    private val watchingDao: WatchingDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val languageManager: LanguageManager,
    private val apiKey: String
) {

    // LRU-кэш для деталей фильмов и сериалов (максимум 100 элементов)
    private val movieDetailsCache = LruCache<Int, MovieResult>(50)
    private val tvShowDetailsCache = LruCache<Int, TvShowResult>(50)
    private val seasonDetailsCache = LruCache<String, TvSeasonDetails>(30)
    
    // Кэш для рекомендаций с TTL
    private data class CachedRecommendations(val data: List<Any>, val timestamp: Long)
    private val recommendationsCache = LruCache<String, CachedRecommendations>(20)
    private val CACHE_TTL_MS = 5 * 60 * 1000L // 5 минут

    private inline fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d("AppRepo", message)
    }

    private inline fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e("AppRepo", message, throwable) else Log.e("AppRepo", message)
    }

    // --- API Methods ---
    suspend fun searchMultiLanguage(query: String, page: Int = 1): List<Any> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()

        // Use current language for search to avoid excessive API calls
        val currentLanguage = languageManager.getApiLanguage()
        
        d("searchMultiLanguage() query='$query' lang=$currentLanguage page=$page")

        // Launch parallel requests for movies and TV shows
        val movieDeferred = async {
            try {
                val resp = api.searchMovies(apiKey, query, currentLanguage, page)
                d("Movie response: lang=$currentLanguage page=$page size=${resp.results.size}")
                resp.results
            } catch (e: Exception) {
                d("Movie request failed: error=${e.message}")
                emptyList()
            }
        }

        val tvDeferred = async {
            try {
                val resp = api.searchTvShows(apiKey, query, currentLanguage, page)
                d("TV response: lang=$currentLanguage page=$page size=${resp.results.size}")
                resp.results
            } catch (e: Exception) {
                d("TV request failed: error=${e.message}")
                emptyList()
            }
        }

        val movieItems = movieDeferred.await()
        val tvItems = tvDeferred.await()

        d("Total items fetched: movies=${movieItems.size} tv=${tvItems.size}")

        // Combine and return (no deduplication needed since single language)
        val result = (movieItems + tvItems) as List<Any>
        d("Returning results count=${result.size}")
        result
    }

    // Details endpoints with caching
    suspend fun getMovieDetails(movieId: Int): MovieResult {
        // Проверяем кэш
        movieDetailsCache.get(movieId)?.let { 
            d("Movie details cache HIT for id=$movieId")
            return it 
        }
        // Загружаем и кэшируем
        d("Movie details cache MISS for id=$movieId")
        return api.getMovieDetails(movieId, apiKey, languageManager.getApiLanguage()).also {
            movieDetailsCache.put(movieId, it)
        }
    }

    suspend fun getTvShowDetails(tvId: Int): TvShowResult {
        // Проверяем кэш
        tvShowDetailsCache.get(tvId)?.let { 
            d("TV details cache HIT for id=$tvId")
            return it 
        }
        // Загружаем и кэшируем
        d("TV details cache MISS for id=$tvId")
        return api.getTvShowDetails(tvId, apiKey, languageManager.getApiLanguage()).also {
            tvShowDetailsCache.put(tvId, it)
        }
    }

    suspend fun getPopularMovies(): MoviesResponse {
        return api.getPopularMovies(apiKey, languageManager.getApiLanguage())
    }

    suspend fun getPopularTvShows(): TvShowsResponse {
        return api.getPopularTvShows(apiKey, languageManager.getApiLanguage())
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
     * С кэшированием
     */
    suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): TvSeasonDetails {
        val cacheKey = "${tvId}_$seasonNumber"
        // Проверяем кэш
        seasonDetailsCache.get(cacheKey)?.let {
            d("Season details cache HIT for tv=$tvId season=$seasonNumber")
            return it
        }
        // Загружаем и кэшируем
        d("Season details cache MISS for tv=$tvId season=$seasonNumber")
        return api.getSeasonDetails(tvId, seasonNumber, apiKey, languageManager.getApiLanguage()).also {
            seasonDetailsCache.put(cacheKey, it)
        }
    }

    /**
     * Отримує детальну інформацію про епізод
     */
    suspend fun getTvEpisodeDetails(tvId: Int, seasonNumber: Int, episodeNumber: Int): TvEpisodeDetails {
        return api.getEpisodeDetails(tvId, seasonNumber, episodeNumber, apiKey, languageManager.getApiLanguage())
    }

    /**
     * Отримує детальну інформацію про всі сезони серіалу з епізодами
     */
    suspend fun getAllSeasonsDetails(tvId: Int, totalSeasons: Int): List<TvSeasonDetails> {
        Log.d("AppRepo", "getAllSeasonsDetails: tvId=$tvId, totalSeasons=$totalSeasons")
        val seasons = mutableListOf<TvSeasonDetails>()

        for (seasonNumber in 1..totalSeasons) {
            try {
                val seasonDetails = getTvSeasonDetails(tvId, seasonNumber) // Call the updated function
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

    suspend fun incrementWatchCount(id: Int, mediaType: String) {
        Log.d("AppRepository", "Incrementing watch count for: id=$id, mediaType=$mediaType")
        try {
            val currentItem = dao.getWatchedItem(id, mediaType)
            if (currentItem != null) {
                val updatedItem = currentItem.copy(
                    watchCount = currentItem.watchCount + 1,
                    lastUpdated = System.currentTimeMillis() // Оновлюємо час останнього перегляду
                )
                dao.update(updatedItem)
                Log.d("AppRepository", "Successfully incremented watch count to: ${updatedItem.watchCount}")
            } else {
                Log.e("AppRepository", "Item not found for rewatch: id=$id, mediaType=$mediaType")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to increment watch count: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteWatchedItem(item: WatchedItem) {
        dao.deleteById(item.id, item.mediaType)
    }
    
    // Update existing watched item (for rating updates, etc.)
    suspend fun updateWatchedItem(item: WatchedItem) {
        dao.update(item)
        Log.d("AppRepository", "Updated watched item: id=${item.id}, rating=${item.userRating}")
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

    /**
     * Consolidates all "seen" or "interacted with" item IDs across all local lists.
     * Used for discovery optimization (deprioritizing seen items in search/dash recommendations).
     */
    suspend fun getAllSeenItemIds(): List<com.example.movietime.data.db.MediaId> {
        val seenIds = mutableListOf<com.example.movietime.data.db.MediaId>()
        try {
            seenIds.addAll(dao.getAllIds())
            seenIds.addAll(plannedDao.getAllIds())
            seenIds.addAll(watchingDao.getAllIds())
            seenIds.addAll(searchHistoryDao.getAllIds())
        } catch (e: Exception) {
            Log.e("AppRepository", "Error collecting seen item IDs", e)
        }
        return seenIds.distinct()
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
        val result = allItems.sortedByDescending { it.timestamp }
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

    // ============ ADVANCED SEARCH / DISCOVER ============

    suspend fun getMovieGenres(language: String = "uk-UA"): List<com.example.movietime.data.model.Genre> {
        return try {
            val response = api.getMovieGenres(apiKey, language)
            d("getMovieGenres success: ${response.genres.size} genres")
            response.genres
        } catch (e: Exception) {
            e("getMovieGenres failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTvGenres(language: String = "uk-UA"): List<com.example.movietime.data.model.Genre> {
        return try {
            val response = api.getTvGenres(apiKey, language)
            d("getTvGenres success: ${response.genres.size} genres")
            response.genres
        } catch (e: Exception) {
            e("getTvGenres failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPersonDetails(personId: Int): com.example.movietime.data.model.PersonDetails? {
        return try {
            api.getPersonDetails(personId, apiKey, "uk-UA")
        } catch (e: Exception) {
            e("getPersonDetails failed: ${e.message}", e)
            null
        }
    }

    suspend fun getPersonCombinedCredits(personId: Int): com.example.movietime.data.model.PersonCombinedCredits? {
        return try {
            api.getPersonCombinedCredits(personId, apiKey, "uk-UA")
        } catch (e: Exception) {
            e("getPersonCombinedCredits failed: ${e.message}", e)
            null
        }
    }

    suspend fun getAllGenres(language: String = "uk-UA"): List<com.example.movietime.data.model.Genre> = coroutineScope {
        val movieGenres = async { getMovieGenres(language) }
        val tvGenres = async { getTvGenres(language) }
        
        val allGenres = (movieGenres.await() + tvGenres.await())
            .distinctBy { it.id }
            .sortedBy { it.name }
        
        d("getAllGenres: ${allGenres.size} unique genres")
        allGenres
    }

    suspend fun searchPeople(query: String, language: String = "uk-UA", page: Int = 1): List<com.example.movietime.data.model.Person> {
        if (query.isBlank()) return emptyList()
        return try {
            val response = api.searchPeople(apiKey, query, language, page)
            d("searchPeople success: ${response.results.size} results for '$query' page=$page")
            response.results
        } catch (e: Exception) {
            e("searchPeople failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchCompanies(query: String): List<CompanyResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val response = api.searchCompanies(apiKey, query)
            d("searchCompanies success: ${response.results.size} results for '$query'")
            response.results
        } catch (e: Exception) {
            e("searchCompanies failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPopularPeople(language: String = "uk-UA"): List<com.example.movietime.data.model.Person> {
        return try {
            val response = api.getPopularPeople(apiKey, language)
            d("getPopularPeople success: ${response.results.size} people")
            response.results
        } catch (e: Exception) {
            e("getPopularPeople failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPersonDetails(personId: Int, language: String = "uk-UA"): com.example.movietime.data.model.PersonDetails? {
        return try {
            val result = api.getPersonDetails(personId, apiKey, language)
            d("getPersonDetails success: ${result.name}")
            result
        } catch (e: Exception) {
            e("getPersonDetails failed: ${e.message}", e)
            null
        }
    }

    suspend fun getMovieCredits(movieId: Int, language: String = "uk-UA"): com.example.movietime.data.model.CreditsResponse? {
        return try {
            val result = api.getMovieCredits(movieId, apiKey, language)
            d("getMovieCredits success: ${result.cast.size} cast, ${result.crew.size} crew")
            result
        } catch (e: Exception) {
            e("getMovieCredits failed: ${e.message}", e)
            null
        }
    }

    suspend fun getTvCredits(tvId: Int, language: String = "uk-UA"): com.example.movietime.data.model.CreditsResponse? {
        return try {
            val result = api.getTvCredits(tvId, apiKey, language)
            d("getTvCredits success: ${result.cast.size} cast, ${result.crew.size} crew")
            result
        } catch (e: Exception) {
            e("getTvCredits failed: ${e.message}", e)
            null
        }
    }

    suspend fun getCollectionDetails(collectionId: Int, language: String = "uk-UA"): com.example.movietime.data.model.CollectionDetails? {
        return try {
            val result = api.getCollectionDetails(collectionId, apiKey, language)
            d("getCollectionDetails success: ${result.name} with ${result.parts.size} parts")
            result
        } catch (e: Exception) {
            e("getCollectionDetails failed: ${e.message}", e)
            null
        }
    }

    suspend fun discoverMovies(
        genreIds: List<Int>? = null,
        personId: Int? = null,
        personRole: com.example.movietime.data.model.PersonRole = com.example.movietime.data.model.PersonRole.ANY,
        minRating: Float? = null,
        maxRating: Float? = null,
        year: Int? = null,
        companyId: Int? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1
    ): List<MovieResult> {
        return try {
            val genresStr = genreIds?.joinToString(",")
            
            // Determine person filter based on role
            val withCast = when (personRole) {
                com.example.movietime.data.model.PersonRole.ACTOR -> personId?.toString()
                com.example.movietime.data.model.PersonRole.ANY -> personId?.toString()
                else -> null
            }
            val withCrew = when (personRole) {
                com.example.movietime.data.model.PersonRole.DIRECTOR,
                com.example.movietime.data.model.PersonRole.WRITER,
                com.example.movietime.data.model.PersonRole.PRODUCER -> personId?.toString()
                com.example.movietime.data.model.PersonRole.ANY -> personId?.toString()
                else -> null
            }
            
            val response = api.discoverMovies(
                apiKey = apiKey,
                language = languageManager.getApiLanguage(),
                page = page,
                sortBy = sortBy,
                withGenres = genresStr,
                withCast = withCast,
                withCrew = withCrew,
                withCompanies = companyId?.toString(),
                voteAverageGte = minRating,
                voteAverageLte = maxRating,
                year = year
            )
            
            d("discoverMovies success: ${response.results.size} results (genres=$genresStr, person=$personId, role=$personRole, company=$companyId)")
            response.results
        } catch (e: Exception) {
            e("discoverMovies failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun discoverTvShows(
        genreIds: List<Int>? = null,
        personId: Int? = null,
        personRole: com.example.movietime.data.model.PersonRole = com.example.movietime.data.model.PersonRole.ANY,
        minRating: Float? = null,
        maxRating: Float? = null,
        companyId: Int? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1
    ): List<TvShowResult> {
        return try {
            val genresStr = genreIds?.joinToString(",")
            
            // Determine person filter based on role (same logic as discoverMovies)
            val withCast = when (personRole) {
                com.example.movietime.data.model.PersonRole.ACTOR -> personId?.toString()
                com.example.movietime.data.model.PersonRole.ANY -> personId?.toString()
                else -> null
            }
            val withCrew = when (personRole) {
                com.example.movietime.data.model.PersonRole.DIRECTOR,
                com.example.movietime.data.model.PersonRole.WRITER,
                com.example.movietime.data.model.PersonRole.PRODUCER -> personId?.toString()
                com.example.movietime.data.model.PersonRole.ANY -> personId?.toString()
                else -> null
            }
            
            val response = api.discoverTvShows(
                apiKey = apiKey,
                language = languageManager.getApiLanguage(),
                page = page,
                sortBy = sortBy,
                withGenres = genresStr,
                withCast = withCast,
                withCrew = withCrew,
                withCompanies = companyId?.toString(),
                voteAverageGte = minRating,
                voteAverageLte = maxRating
            )
            
            d("discoverTvShows success: ${response.results.size} results (genres=$genresStr, person=$personId, role=$personRole, company=$companyId)")
            response.results
        } catch (e: Exception) {
            e("discoverTvShows failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun discoverByFilters(
        mediaType: String, // "movie", "tv", or "all"
        genreIds: List<Int>? = null,
        personId: Int? = null,
        personRole: com.example.movietime.data.model.PersonRole = com.example.movietime.data.model.PersonRole.ANY,
        minRating: Float? = null,
        maxRating: Float? = null,
        companyId: Int? = null,
        year: Int? = null,
        sortBy: String = "popularity.desc"
    ): List<Any> = coroutineScope {
        val results = mutableListOf<Any>()
        
        when (mediaType) {
            "movie" -> {
                results.addAll(discoverMovies(genreIds, personId, personRole, minRating, maxRating, year, companyId, sortBy))
            }
            "tv" -> {
                results.addAll(discoverTvShows(genreIds, personId, personRole, minRating, maxRating, companyId, sortBy))
            }
            else -> { // "all"
                val movies = async { discoverMovies(genreIds, personId, personRole, minRating, maxRating, year, companyId, sortBy) }
                val tvShows = async { discoverTvShows(genreIds, personId, personRole, minRating, maxRating, companyId, sortBy) }
                
                results.addAll(movies.await())
                results.addAll(tvShows.await())
                
                // Sort combined results by the same criteria
                when {
                    sortBy.contains("popularity") -> results.sortByDescending {
                        when (it) {
                            is MovieResult -> it.popularity
                            is TvShowResult -> it.popularity
                            else -> 0f
                        }
                    }
                    sortBy.contains("vote_average") -> results.sortByDescending {
                        when (it) {
                            is MovieResult -> it.voteAverage
                            is TvShowResult -> it.voteAverage
                            else -> 0f
                        }
                    }
                }
            }
        }
        
        d("discoverByFilters: ${results.size} total results for mediaType=$mediaType")
        results
    }

    suspend fun getPersonMovieCredits(personId: Int, language: String = "uk-UA"): com.example.movietime.data.model.PersonMovieCredits? {
        return try {
            val result = api.getPersonMovieCredits(personId, apiKey, language)
            d("getPersonMovieCredits success: ${result.cast.size} cast, ${result.crew.size} crew")
            result
        } catch (e: Exception) {
            e("getPersonMovieCredits failed: ${e.message}", e)
            null
        }
    }

    suspend fun getPersonTvCredits(personId: Int, language: String = "uk-UA"): com.example.movietime.data.model.PersonTvCredits? {
        return try {
            val result = api.getPersonTvCredits(personId, apiKey, language)
            d("getPersonTvCredits success: ${result.cast.size} cast, ${result.crew.size} crew")
            result
        } catch (e: Exception) {
            e("getPersonTvCredits failed: ${e.message}", e)
            null
        }
    }

    // --- Backup Methods ---
    suspend fun deleteAllWatched() {
        try {
            dao.deleteAll()
            d("All watched items deleted")
        } catch (e: Exception) {
            e("deleteAllWatched failed: ${e.message}", e)
        }
    }

    suspend fun deleteAllPlanned() {
        try {
            plannedDao.deleteAll()
            d("All planned items deleted")
        } catch (e: Exception) {
            e("deleteAllPlanned failed: ${e.message}", e)
        }
    }

    suspend fun deleteAllWatching() {
        try {
            watchingDao.deleteAll()
            d("All watching items deleted")
        } catch (e: Exception) {
            e("deleteAllWatching failed: ${e.message}", e)
        }
    }

    suspend fun getWatchedItemsForBackup(): List<WatchedItem> {
        return try {
            dao.getAllSync()
        } catch (e: Exception) {
            e("getWatchedItemsForBackup failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPlannedItemsForBackup(): List<PlannedItem> {
        return try {
            plannedDao.getAllSync()
        } catch (e: Exception) {
            e("getPlannedItemsForBackup failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getWatchingItemsForBackup(): List<WatchingItem> {
        return try {
            watchingDao.getAllSync()
        } catch (e: Exception) {
            e("getWatchingItemsForBackup failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun insertPlannedDirect(item: PlannedItem) {
        try {
            plannedDao.insert(item)
            d("Planned item inserted directly: ${item.title}")
        } catch (e: Exception) {
            e("insertPlannedDirect failed: ${e.message}", e)
        }
    }

    suspend fun insertWatchingDirect(item: WatchingItem) {
        try {
            watchingDao.insert(item)
            d("Watching item inserted directly: ${item.title}")
        } catch (e: Exception) {
            e("insertWatchingDirect failed: ${e.message}", e)
        }
    }
}

