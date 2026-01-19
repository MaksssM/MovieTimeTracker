package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Entity for storing search history - items that user clicked on in search results
 */
@Entity(
    tableName = "search_history",
    primaryKeys = ["id", "mediaType"],
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["mediaType"])
    ]
)
data class SearchHistoryItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val mediaType: String, // "movie" or "tv"
    val releaseDate: String?,
    val voteAverage: Double?,
    val timestamp: Long = System.currentTimeMillis()
)
