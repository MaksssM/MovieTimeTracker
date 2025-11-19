package com.example.movietime.data.db

import androidx.room.Entity

@Entity(tableName = "watching_items", primaryKeys = ["id", "mediaType"])
data class WatchingItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val currentEpisode: Int? = null,
    val currentSeason: Int? = null
)
