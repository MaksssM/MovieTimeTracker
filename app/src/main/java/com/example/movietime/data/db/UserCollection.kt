package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-created collection of movies/TV shows.
 * Like playlists in Spotify, users can create custom lists.
 */
@Entity(tableName = "user_collections")
data class UserCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverImagePath: String? = null,
    val emoji: String? = null, // Optional emoji icon for the collection
    val color: Int? = null, // Optional color theme
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
