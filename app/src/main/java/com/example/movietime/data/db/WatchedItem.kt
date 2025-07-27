package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_items")
data class WatchedItem(
    @PrimaryKey
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int,
    val mediaType: String
)