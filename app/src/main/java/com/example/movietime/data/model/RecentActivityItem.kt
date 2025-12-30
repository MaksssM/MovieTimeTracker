package com.example.movietime.data.model

sealed class RecentActivityItem {
    abstract val id: Int
    abstract val title: String
    abstract val timestamp: Long
    abstract val type: ActivityType
    abstract val mediaType: String
    open val posterPath: String? = null

    data class Watched(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String,
        override val posterPath: String? = null
    ) : RecentActivityItem() {
        override val type = ActivityType.WATCHED
    }

    data class Planned(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String,
        override val posterPath: String? = null
    ) : RecentActivityItem() {
        override val type = ActivityType.PLANNED
    }

    data class Watching(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String,
        override val posterPath: String? = null
    ) : RecentActivityItem() {
        override val type = ActivityType.WATCHING
    }

    /**
     * Item that user searched for and clicked on in search results
     */
    data class Searched(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String,
        override val posterPath: String? = null,
        val voteAverage: Double? = null
    ) : RecentActivityItem() {
        override val type = ActivityType.SEARCHED
    }

    enum class ActivityType {
        WATCHED, PLANNED, WATCHING, SEARCHED
    }
}
