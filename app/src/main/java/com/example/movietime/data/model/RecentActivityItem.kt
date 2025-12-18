package com.example.movietime.data.model

sealed class RecentActivityItem {
    abstract val id: Int
    abstract val title: String
    abstract val timestamp: Long
    abstract val type: ActivityType
    abstract val mediaType: String

    data class Watched(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String
    ) : RecentActivityItem() {
        override val type = ActivityType.WATCHED
    }

    data class Planned(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String
    ) : RecentActivityItem() {
        override val type = ActivityType.PLANNED
    }

    data class Watching(
        override val id: Int,
        override val title: String,
        override val timestamp: Long,
        override val mediaType: String
    ) : RecentActivityItem() {
        override val type = ActivityType.WATCHING
    }

    enum class ActivityType {
        WATCHED, PLANNED, WATCHING
    }
}
