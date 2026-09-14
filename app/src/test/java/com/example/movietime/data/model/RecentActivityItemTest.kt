package com.example.movietime.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentActivityItemTest {

    @Test
    fun watchedActivityItem_hasCorrectTypeAndProperties() {
        val item = RecentActivityItem.Watched(
            id = 1,
            title = "Inception",
            timestamp = 1620000000000L,
            mediaType = "movie",
            posterPath = "/inception.jpg"
        )

        assertEquals(RecentActivityItem.ActivityType.WATCHED, item.type)
        assertEquals(1, item.id)
        assertEquals("Inception", item.title)
        assertEquals("movie", item.mediaType)
        assertEquals("/inception.jpg", item.posterPath)
    }

    @Test
    fun plannedActivityItem_hasCorrectType() {
        val item = RecentActivityItem.Planned(
            id = 2,
            title = "Interstellar",
            timestamp = 1620000000000L,
            mediaType = "movie"
        )

        assertEquals(RecentActivityItem.ActivityType.PLANNED, item.type)
        assertNull(item.posterPath)
    }

    @Test
    fun watchingActivityItem_hasCorrectType() {
        val item = RecentActivityItem.Watching(
            id = 3,
            title = "Stranger Things",
            timestamp = 1620000000000L,
            mediaType = "tv",
            posterPath = "/st.jpg"
        )

        assertEquals(RecentActivityItem.ActivityType.WATCHING, item.type)
        assertEquals("tv", item.mediaType)
    }

    @Test
    fun searchedActivityItem_hasCorrectTypeAndVoteAverage() {
        val item = RecentActivityItem.Searched(
            id = 4,
            title = "The Dark Knight",
            timestamp = 1620000000000L,
            mediaType = "movie",
            voteAverage = 9.0
        )

        assertEquals(RecentActivityItem.ActivityType.SEARCHED, item.type)
        assertEquals(9.0, item.voteAverage ?: 0.0, 0.001)
    }
}