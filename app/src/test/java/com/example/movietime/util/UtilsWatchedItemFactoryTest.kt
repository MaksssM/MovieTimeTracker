package com.example.movietime.util

import com.example.movietime.data.model.TvShowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UtilsWatchedItemFactoryTest {

    @Test
    fun createWatchedItemFromMovie_mapsFieldsCorrectly() {
        val item = Utils.createWatchedItemFromMovie(
            id = 550,
            title = "Fight Club",
            name = null,
            posterPath = "/poster.jpg",
            releaseDate = "1999-10-15",
            runtime = 139,
            mediaType = "movie",
            overview = "An insomniac office worker...",
            voteAverage = 8.4,
            userRating = 9.0f,
            genreIds = listOf(18, 53)
        )

        assertEquals(550, item.id)
        assertEquals("Fight Club", item.title)
        assertEquals("/poster.jpg", item.posterPath)
        assertEquals("1999-10-15", item.releaseDate)
        assertEquals(139, item.runtime)
        assertEquals("movie", item.mediaType)
        assertEquals("18,53", item.genreIds)
        assertEquals(9.0f, item.userRating)
        assertNotNull(item.lastUpdated)
    }

    @Test
    fun createWatchedItemFromMovie_nullTitle_fallsBackToNameOrFallback() {
        val item = Utils.createWatchedItemFromMovie(
            id = 1,
            title = null,
            name = "Test Name",
            posterPath = null,
            releaseDate = null,
            runtime = null,
            mediaType = "movie"
        )
        assertEquals("Test Name", item.title)
        assertEquals(0, item.runtime)

        val fallbackItem = Utils.createWatchedItemFromMovie(
            id = 2,
            title = null,
            name = null,
            posterPath = null,
            releaseDate = null,
            runtime = null,
            mediaType = "movie"
        )
        assertEquals("Без назви", fallbackItem.title)
    }

    @Test
    fun createWatchedItemFromTvShow_withExplicitValues() {
        val tvShow = TvShowResult(
            id = 200,
            name = "Better Call Saul",
            overview = "Lawyer drama",
            posterPath = "/saul.jpg",
            backdropPath = null,
            firstAirDate = "2015-02-08",
            lastAirDate = null,
            voteAverage = 8.6f,
            status = "Ended"
        )

        val item = Utils.createWatchedItemFromTvShow(
            tvShow = tvShow,
            episodeRuntime = 50,
            totalEpisodes = 63,
            userRating = 9.5f
        )

        assertEquals(200, item.id)
        assertEquals("Better Call Saul", item.title)
        assertEquals("tv", item.mediaType)
        assertEquals(50, item.episodeRuntime)
        assertEquals(63, item.totalEpisodes)
        assertEquals(3150, item.runtime) // 50 * 63
        assertEquals(9.5f, item.userRating)
    }
}