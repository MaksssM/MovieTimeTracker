package com.example.movietime.util

import android.content.Context
import com.example.movietime.data.db.WatchedItem

object Utils {
    fun formatMinutesToHoursAndMinutes(minutes: Int?): String {
        val mins = minutes ?: 0
        if (mins <= 0) {
            return "0 год 0 хв"
        }
        val hours = mins / 60
        val remainingMinutes = mins % 60

        // Use short Ukrainian labels (abbreviations) for compactness
        return "${hours} год ${remainingMinutes} хв"
    }

    /**
     * Compute total runtime for a TV show given episode runtime and number of watched episodes.
     * Defensive: treats null episodeRuntime as 0 and negative episodes as 0.
     */
    fun computeTotalRuntimeForTv(episodeRuntime: Int?, episodes: Int): Int {
        val ep = episodeRuntime ?: 0
        val eps = if (episodes < 0) 0 else episodes
        return ep * eps
    }

    fun createWatchedItemFromMovie(
        id: Int,
        title: String?,
        name: String?,
        posterPath: String?,
        releaseDate: String?,
        runtime: Int?,
        mediaType: String,
        overview: String? = null,
        voteAverage: Double? = null,
        userRating: Float? = null
    ): WatchedItem {
        return WatchedItem(
            id = id,
            title = title ?: name ?: "Без назви",
            posterPath = posterPath,
            releaseDate = releaseDate,
            runtime = runtime ?: 0,
            mediaType = mediaType,
            overview = overview,
            voteAverage = voteAverage,
            userRating = userRating
        )
    }
}