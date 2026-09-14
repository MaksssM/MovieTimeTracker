package com.example.movietime.util

import com.example.movietime.data.model.TvEpisodeDetails
import com.example.movietime.data.model.TvSeasonDetails
import com.example.movietime.data.model.TvShowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTvAutoComputeTest {

    @Test
    fun autoComputeTvShowRuntime_miniSeries_detected() {
        val tvShow = TvShowResult(
            id = 101,
            name = "Chernobyl",
            overview = "Mini-series",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2019-05-06",
            lastAirDate = null,
            numberOfSeasons = 1,
            numberOfEpisodes = 5,
            episodeRunTime = listOf(60)
        )

        val result = Utils.autoComputeTvShowRuntime(tvShow)

        assertEquals("Міні-серіал", result.description)
        assertEquals(60, result.episodeRuntime)
        assertEquals(5, result.episodes)
        assertEquals(300, result.totalMinutes)
    }

    @Test
    fun autoComputeTvShowRuntime_ongoingSeries_detected() {
        val tvShow = TvShowResult(
            id = 102,
            name = "The Boys",
            overview = "Ongoing action show",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2019-07-26",
            lastAirDate = null,
            status = "Returning Series",
            numberOfSeasons = 4,
            numberOfEpisodes = 32,
            episodeRunTime = listOf(60)
        )

        val result = Utils.autoComputeTvShowRuntime(tvShow)

        assertEquals("Серіал що виходить", result.description)
        assertEquals(32, result.episodes)
        assertEquals(60, result.episodeRuntime)
        assertEquals(1920, result.totalMinutes)
        assertTrue(result.isOngoing)
    }

    @Test
    fun autoComputeTvShowRuntime_endedSeries_detected() {
        val tvShow = TvShowResult(
            id = 103,
            name = "Breaking Bad",
            overview = "Completed show",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2008-01-20",
            lastAirDate = "2013-09-29",
            status = "Ended",
            numberOfSeasons = 5,
            numberOfEpisodes = 62,
            episodeRunTime = listOf(47)
        )

        val result = Utils.autoComputeTvShowRuntime(tvShow)

        assertEquals("Закінчений серіал", result.description)
        assertEquals(62, result.episodes)
        assertEquals(47, result.episodeRuntime)
        assertEquals(47 * 62, result.totalMinutes)
        assertFalse(result.isOngoing)
    }

    @Test
    fun autoComputeTvShowRuntime_comedyGenreEstimation() {
        val tvShow = TvShowResult(
            id = 105,
            name = "Brooklyn Nine-Nine",
            overview = "Comedy series",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2013-09-17",
            lastAirDate = null,
            status = "Ended",
            numberOfSeasons = 8,
            numberOfEpisodes = 153,
            episodeRunTime = null,
            genreIds = listOf(35) // Comedy -> 22 min
        )

        val result = Utils.autoComputeTvShowRuntime(tvShow)

        assertEquals(22, result.episodeRuntime)
        assertEquals(153, result.episodes)
        assertEquals(22 * 153, result.totalMinutes)
    }

    @Test
    fun computeExactTvShowRuntime_calculatesAccurateStats() {
        val season1 = TvSeasonDetails(
            airDate = "2020-01-01",
            id = 1,
            name = "Season 1",
            overview = null,
            posterPath = null,
            seasonNumber = 1,
            episodes = listOf(
                TvEpisodeDetails(airDate = "2020-01-01", episodeNumber = 1, id = 1, name = "Ep 1", overview = null, runtime = 40, seasonNumber = 1, stillPath = null, voteAverage = 8f, voteCount = 100),
                TvEpisodeDetails(airDate = "2020-01-08", episodeNumber = 2, id = 2, name = "Ep 2", overview = null, runtime = 60, seasonNumber = 1, stillPath = null, voteAverage = 8f, voteCount = 100)
            )
        )
        val season2 = TvSeasonDetails(
            airDate = "2021-01-01",
            id = 2,
            name = "Season 2",
            overview = null,
            posterPath = null,
            seasonNumber = 2,
            episodes = listOf(
                TvEpisodeDetails(airDate = "2021-01-01", episodeNumber = 1, id = 3, name = "Ep 3", overview = null, runtime = 50, seasonNumber = 2, stillPath = null, voteAverage = 8f, voteCount = 100)
            )
        )

        val exactRuntimeInfo = Utils.computeExactTvShowRuntime(listOf(season1, season2))

        assertEquals(150, exactRuntimeInfo.totalMinutes)
        assertEquals(3, exactRuntimeInfo.totalEpisodes)
        assertEquals(3, exactRuntimeInfo.episodesWithRuntime)
        assertEquals(50, exactRuntimeInfo.averageEpisodeRuntime)
        assertEquals(40, exactRuntimeInfo.minEpisodeRuntime)
        assertEquals(60, exactRuntimeInfo.maxEpisodeRuntime)
        assertEquals(20, exactRuntimeInfo.runtimeVariance)
        assertEquals(100, exactRuntimeInfo.completionPercentage)
        assertTrue(exactRuntimeInfo.hasVariableRuntime)
        assertTrue(exactRuntimeInfo.isComplete)
    }

    @Test
    fun computeExactTvShowRuntime_emptySeasons_handlesSafely() {
        val exactRuntimeInfo = Utils.computeExactTvShowRuntime(emptyList())

        assertEquals(0, exactRuntimeInfo.totalMinutes)
        assertEquals(0, exactRuntimeInfo.totalEpisodes)
        assertEquals(0, exactRuntimeInfo.averageEpisodeRuntime)
        assertEquals(0, exactRuntimeInfo.completionPercentage)
        assertFalse(exactRuntimeInfo.hasVariableRuntime)
        assertTrue(exactRuntimeInfo.isComplete)
    }
}