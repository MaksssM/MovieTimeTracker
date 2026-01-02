package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a person (actor/director) the user is following.
 * Used for notifications about new releases.
 */
@Entity(tableName = "followed_people")
data class FollowedPerson(
    @PrimaryKey
    val personId: Int,
    val name: String,
    val profilePath: String? = null,
    val knownForDepartment: String? = null, // "Acting", "Directing", etc.
    val followedAt: Long = System.currentTimeMillis(),
    val notificationsEnabled: Boolean = true
)
