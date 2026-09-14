package com.example.movietime.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsModelsTest {

    @Test
    fun detailedStatistics_defaultValues() {
        val stats = DetailedStatistics()

        assertEquals(0L, stats.totalWatchTimeMinutes)
        assertEquals(0, stats.totalMovies)
        assertEquals(0, stats.totalTvShows)
        assertEquals(0, stats.totalTvEpisodes)
        assertEquals(0f, stats.averageMovieRating, 0.001f)
        assertEquals(0f, stats.averageTvRating, 0.001f)
        assertEquals(0.5f, stats.movieVsTvRatio, 0.001f)
        assertEquals(emptyList<GenreStatItem>(), stats.favoriteGenres)
        assertEquals(emptyList<DirectorStatItem>(), stats.favoriteDirectors)
        assertEquals(emptyList<ActorStatItem>(), stats.favoriteActors)
        assertNull(stats.longestMovieWatched)
        assertNull(stats.longestTvShow)
        assertNull(stats.mostRewatchedItem)
    }

    @Test
    fun genreStatItem_properties() {
        val genreStat = GenreStatItem(
            genreId = 28,
            genreName = "Action",
            count = 15,
            totalWatchTimeMinutes = 1800,
            percentage = 25.5f,
            averageRating = 8.2f
        )

        assertEquals(28, genreStat.genreId)
        assertEquals("Action", genreStat.genreName)
        assertEquals(15, genreStat.count)
        assertEquals(1800L, genreStat.totalWatchTimeMinutes)
        assertEquals(25.5f, genreStat.percentage, 0.001f)
        assertEquals(8.2f, genreStat.averageRating, 0.001f)
    }

    @Test
    fun directorStatItem_properties() {
        val directorStat = DirectorStatItem(
            directorId = 525,
            directorName = "Christopher Nolan",
            profilePath = "/nolan.jpg",
            moviesWatched = 8,
            totalWatchTimeMinutes = 1200,
            averageRating = 9.1f,
            movieTitles = listOf("Inception", "Interstellar", "Oppenheimer")
        )

        assertEquals(525, directorStat.directorId)
        assertEquals("Christopher Nolan", directorStat.directorName)
        assertEquals("/nolan.jpg", directorStat.profilePath)
        assertEquals(8, directorStat.moviesWatched)
        assertEquals(3, directorStat.movieTitles.size)
    }

    @Test
    fun bestMonthItem_properties() {
        val bestMonth = BestMonthItem(
            monthName = "January",
            year = 2026,
            count = 12,
            watchTimeMinutes = 1500
        )

        assertEquals("January", bestMonth.monthName)
        assertEquals(2026, bestMonth.year)
        assertEquals(12, bestMonth.count)
        assertEquals(1500L, bestMonth.watchTimeMinutes)
    }
}