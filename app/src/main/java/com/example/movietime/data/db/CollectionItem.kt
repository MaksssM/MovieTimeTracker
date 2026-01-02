package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table linking collections to movies/TV shows.
 * Supports the many-to-many relationship.
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "itemId", "mediaType"],
    foreignKeys = [
        ForeignKey(
            entity = UserCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectionId"])]
)
data class CollectionItem(
    val collectionId: Long,
    val itemId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String? = null, // Cached for display
    val posterPath: String? = null, // Cached for display
    val addedAt: Long = System.currentTimeMillis()
)
