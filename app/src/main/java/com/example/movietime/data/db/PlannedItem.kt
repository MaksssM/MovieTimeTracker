package com.example.movietime.data.db

import androidx.room.Entity

@Entity(tableName = "planned_items", primaryKeys = ["id", "mediaType"])
data class PlannedItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val dateAdded: Long = System.currentTimeMillis()
)
