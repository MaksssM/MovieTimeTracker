package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_items_table")
data class WatchedItem(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val runtimeInMinutes: Int,
    val mediaType: String // "movie" или "tv"
)