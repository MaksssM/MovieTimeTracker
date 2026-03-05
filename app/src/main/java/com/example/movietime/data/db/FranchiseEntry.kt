package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual entry within a saga or universe.
 *
 * entryType:
 *   TMDB_COLLECTION  — a TMDB official collection (e.g. Iron Man trilogy, id=131292)
 *   STANDALONE_MOVIE — a single movie not part of a TMDB collection
 *   STANDALONE_TV    — a TV show
 *
 * relationshipType:
 *   CORE     — main storyline (Avengers, Iron Man…)
 *   SPIN_OFF — derived story sharing characters (Deadpool, Venom…)
 *   RELATED  — set in same universe but separate continuity (X-Men pre-MCU…)
 *
 * movieIds — comma-separated TMDB movie IDs that belong to this entry;
 *            used to compute watch progress without an API call.
 */
@Entity(
    tableName = "franchise_entries",
    foreignKeys = [
        ForeignKey(
            entity = CinematicUniverse::class,
            parentColumns = ["id"],
            childColumns = ["universeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FranchiseSaga::class,
            parentColumns = ["id"],
            childColumns = ["sagaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("universeId"), Index("sagaId")]
)
data class FranchiseEntry(
    @PrimaryKey val id: Long,
    val universeId: Long,
    val sagaId: Long? = null,        // null = not assigned to a saga (shown in "Related")
    val name: String,
    val entryType: String,           // TMDB_COLLECTION | STANDALONE_MOVIE | STANDALONE_TV
    val tmdbCollectionId: Int? = null,
    val tmdbMediaId: Int? = null,    // mediaId for standalone entries
    val mediaType: String? = null,   // "movie" | "tv"
    val displayOrder: Int = 0,
    val relationshipType: String = "CORE",  // CORE | SPIN_OFF | RELATED
    val movieIds: String = "",       // comma-separated TMDB IDs for progress tracking
    val totalCount: Int = 1,         // total number of movies/episodes in this entry
    val note: String? = null         // e.g. "Disney+" or "Animated" or "Phase 4"
)
