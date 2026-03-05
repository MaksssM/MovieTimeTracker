package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mid-level grouping within a universe.
 * E.g.  MCU → "Infinity Saga", MCU → "Multiverse Saga"
 *       Star Wars → "Original Trilogy", "Prequel Trilogy"
 */
@Entity(
    tableName = "franchise_sagas",
    foreignKeys = [
        ForeignKey(
            entity = CinematicUniverse::class,
            parentColumns = ["id"],
            childColumns = ["universeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("universeId")]
)
data class FranchiseSaga(
    @PrimaryKey val id: Long,
    val universeId: Long,
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val yearRange: String? = null // e.g. "2008–2019"
)
