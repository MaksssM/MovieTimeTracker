package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Top-level grouping: e.g. MCU, DCEU, Star Wars, Harry Potter
 */
@Entity(tableName = "cinematic_universes")
data class CinematicUniverse(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String? = null,
    val backdropPath: String? = null,   // TMDB backdrop URL or null
    val posterPath: String? = null,
    val logoEmoji: String = "🎬",
    val accentColorHex: String? = null, // e.g. "#C0392B" for MCU red
    val isSeeded: Boolean = true         // pre-populated by app (vs user-created)
)
