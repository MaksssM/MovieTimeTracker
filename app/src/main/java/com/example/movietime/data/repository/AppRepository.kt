package com.example.movietime.data.repository

import androidx.lifecycle.LiveData
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.model.ApiMediaItem
import com.example.movietime.data.model.ApiMovieDetails
import com.example.movietime.data.model.ApiTvShowDetails

class AppRepository(
    private val watchedItemDao: WatchedItemDao,
    private val tmdbApi: TmdbApi
) {
    val allWatchedItems: LiveData<List<WatchedItem>> = watchedItemDao.getAllWatchedItems()
    val totalWatchTime: LiveData<Int?> = watchedItemDao.getTotalWatchTimeInMinutes()

    suspend fun search(query: String): List<ApiMediaItem> {
        return try {
            val response = tmdbApi.search(query)
            response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetails(movieId: Int): ApiMovieDetails? {
        return try { tmdbApi.getMovieDetails(movieId) } catch (e: Exception) { null }
    }

    suspend fun getTvShowDetails(tvId: Int): ApiTvShowDetails? {
        return try { tmdbApi.getTvShowDetails(tvId) } catch (e: Exception) { null }
    }

    fun isItemWatched(id: Int): LiveData<WatchedItem?> {
        return watchedItemDao.getWatchedItemById(id)
    }

    suspend fun addWatchedItem(item: WatchedItem) {
        watchedItemDao.addWatchedItem(item)
    }

    suspend fun deleteWatchedItem(id: Int) {
        watchedItemDao.deleteItemById(id)
    }
}