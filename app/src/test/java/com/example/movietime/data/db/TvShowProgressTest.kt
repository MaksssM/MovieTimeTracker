package com.example.movietime.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvShowProgressTest {

    @Test
    fun seasonProgress_isComplete_whenAllEpisodesWatched() {
        val completeSeason = SeasonProgress(
            seasonNumber = 1,
            totalEpisodes = 10,
            watchedEpisodes = 10,
            totalRuntime = 450,
            watchedRuntime = 450
        )
        assertTrue(completeSeason.isComplete)
        assertEquals(1.0f, completeSeason.progressPercent, 0.001f)
    }

    @Test
    fun seasonProgress_inProgress_whenPartiallyWatched() {
        val inProgressSeason = SeasonProgress(
            seasonNumber = 2,
            totalEpisodes = 10,
            watchedEpisodes = 5,
            totalRuntime = 500,
            watchedRuntime = 250
        )
        assertFalse(inProgressSeason.isComplete)
        assertEquals(0.5f, inProgressSeason.progressPercent, 0.001f)
    }

    @Test
    fun seasonProgress_zeroEpisodes_handledGracefully() {
        val emptySeason = SeasonProgress(
            seasonNumber = 0,
            totalEpisodes = 0,
            watchedEpisodes = 0,
            totalRuntime = 0,
            watchedRuntime = 0
        )
        assertTrue(emptySeason.isComplete)
        assertEquals(0f, emptySeason.progressPercent, 0.001f)
    }

    @Test
    fun tvShowProgressSummary_aggregatesCorrectly() {
        val season1 = SeasonProgress(1, 10, 10, 450, 450)
        val season2 = SeasonProgress(2, 10, 5, 500, 250)

        val summary = TvShowProgressSummary(
            tvShowId = 42,
            totalSeasons = 2,
            totalEpisodes = 20,
            watchedEpisodes = 15,
            totalRuntime = 950,
            watchedRuntime = 700,
            seasons = listOf(season1, season2)
        )

        assertFalse(summary.isComplete)
        assertEquals(0.75f, summary.progressPercent, 0.001f)
        assertEquals(2, summary.seasons.size)
    }

    @Test
    fun tvShowProgress_entityHoldsAttributes() {
        val progress = TvShowProgress(
            tvShowId = 100,
            seasonNumber = 1,
            episodeNumber = 1,
            episodeName = "Pilot",
            episodeRuntime = 58,
            watched = true,
            watchedAt = 1700000000000L
        )

        assertEquals(100, progress.tvShowId)
        assertEquals(1, progress.seasonNumber)
        assertEquals(1, progress.episodeNumber)
        assertEquals("Pilot", progress.episodeName)
        assertEquals(58, progress.episodeRuntime)
        assertTrue(progress.watched)
        assertEquals(1700000000000L, progress.watchedAt)
    }
}