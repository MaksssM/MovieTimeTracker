package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "watched_items",
    primaryKeys = ["id", "mediaType"],
    indices = [
        Index(value = ["mediaType"]),
        Index(value = ["lastUpdated"]),
        Index(value = ["userRating"])
    ]
)
data class WatchedItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val overview: String? = null,
    val voteAverage: Double? = null,
    val userRating: Float? = null,
    // Нові поля для серіалів
    val episodeRuntime: Int? = null, // Тривалість одного епізоду
    val totalEpisodes: Int? = null,  // Кількість епізодів
    val isOngoing: Boolean = false,  // Чи серіал ще виходить
    val status: String? = null,      // Статус серіалу
    val lastUpdated: Long? = null,   // Час останнього оновлення
    val genreIds: String? = null,    // Comma-separated genre IDs
    val watchCount: Int = 1          // Кількість переглядів (дефолт 1)
)