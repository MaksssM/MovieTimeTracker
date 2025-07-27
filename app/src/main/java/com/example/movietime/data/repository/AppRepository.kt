package com.example.movietime.data.repository

import androidx.lifecycle.LiveData
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.model.ApiMovie
import com.example.movietime.data.model.MoviesResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: TmdbApi,
    private val dao: WatchedItemDao,
    private val apiKey: String
) {

    // --- API Methods ---
    suspend fun searchMovies(query: String): MoviesResponse {
        return api.searchMovies(apiKey, query)
    }

    suspend fun getMovieDetails(movieId: Int): ApiMovie {
        return api.getMovieDetails(movieId, apiKey)
    }

    suspend fun getPopularMovies(): MoviesResponse {
        return api.getPopularMovies(apiKey)
    }

    fun getWatchedItems(): LiveData<List<WatchedItem>> {
        return dao.getAll()
    }

    suspend fun addWatchedItem(item: WatchedItem) {
        dao.insert(item)
    }

    suspend fun deleteWatchedItem(item: WatchedItem) {
        dao.deleteById(item.id)
    }

    suspend fun getWatchedItemById(id: Int): WatchedItem? {
        return dao.getById(id)
    }
}