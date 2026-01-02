package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks each time a user rewatches a movie/TV show.
 * Enables "rewatch counter" and viewing history.
 */
@Entity(tableName = "rewatch_entries")
data class RewatchEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String? = null, // Cached for display
    val posterPath: String? = null,
    val watchedAt: Long = System.currentTimeMillis(),
    val userRating: Float? = null, // User can rate each viewing
    val notes: String? = null, // Optional notes about this viewing
    val watchTimeMinutes: Int? = null // How long they watched (for runtime tracking)
)
