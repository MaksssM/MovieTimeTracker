package com.example.movietime.data.db

import androidx.room.Entity

@Entity(tableName = "watched_items", primaryKeys = ["id", "mediaType"])
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
    val lastUpdated: Long? = null    // Час останнього оновлення
)