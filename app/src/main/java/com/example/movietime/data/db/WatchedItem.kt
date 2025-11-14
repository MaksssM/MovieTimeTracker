package com.example.movietime.data.db

import androidx.room.Entity

@Entity(tableName = "watched_items", primaryKeys = ["id", "mediaType"])
data class WatchedItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String
)