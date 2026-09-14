package com.example.movietime.service

import com.example.movietime.data.db.MediaId
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.MoviesResponse
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.model.TvShowsResponse
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.LanguageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationServiceTest {

    private val repository: AppRepository = mock()
    private val languageManager: LanguageManager = mock()

    private lateinit var recommendationService: RecommendationService

    private fun createWatchedItem(id: Int, title: String, mediaType: String, rating: Float?): WatchedItem {
        return WatchedItem(
            id = id,
            title = title,
            posterPath = null,
            releaseDate = null,
            runtime = 120,
            mediaType = mediaType,
            userRating = rating,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun createMovieResult(id: Int, title: String, voteAverage: Float): MovieResult {
        return MovieResult(
            id = id,
            title = title,
            overview = null,
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            runtime = 120,
            voteAverage = voteAverage,
            popularity = 100f
        )
    }

    private fun createTvShowResult(id: Int, name: String, voteAverage: Float): TvShowResult {
        return TvShowResult(
            id = id,
            name = name,
            overview = null,
            posterPath = null,
            backdropPath = null,
            firstAirDate = null,
            lastAirDate = null,
            voteAverage = voteAverage,
            popularity = 100f
        )
    }

    @Before
    fun setUp() {
        recommendationService = RecommendationService(repository, languageManager)
    }

    @Test
    fun getPersonalizedRecommendations_emptyWatchedItems_returnsPopularFallback() = runTest {
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())

        val fallbackMovies = MoviesResponse(listOf(createMovieResult(100, "Popular Movie 1", 8.0f)))
        val fallbackTvShows = TvShowsResponse(listOf(createTvShowResult(200, "Popular Show 1", 8.5f)))

        whenever(repository.getPopularMovies()).thenReturn(fallbackMovies)
        whenever(repository.getPopularTvShows()).thenReturn(fallbackTvShows)

        val result = recommendationService.getPersonalizedRecommendations()

        assertEquals(1, result.movies.size)
        assertEquals("Popular Movie 1", result.movies[0].title)
        assertEquals(1, result.tvShows.size)
        assertEquals("Popular Show 1", result.tvShows[0].name)
    }

    @Test
    fun getPersonalizedRecommendations_withWatchedItems_fetchesRecommendationsAndFiltersSeen() = runTest {
        val watched = listOf(createWatchedItem(1, "Watched Movie", "movie", 9.0f))
        whenever(repository.getWatchedItemsSync()).thenReturn(watched)
        whenever(repository.getAllSeenItemIds()).thenReturn(listOf(MediaId(1, "movie"), MediaId(100, "movie")))

        val recommendedFromMovie1 = listOf(
            createMovieResult(100, "Already Seen Movie", 8.0f),
            createMovieResult(101, "New Recommended Movie", 7.5f),
            createMovieResult(102, "Low Rating Movie", 4.0f) // < 5.5, should be filtered out
        )

        whenever(repository.getMovieRecommendations(eq(1))).thenReturn(recommendedFromMovie1)
        whenever(repository.getPopularMovies()).thenReturn(MoviesResponse(emptyList()))
        whenever(repository.getPopularTvShows()).thenReturn(TvShowsResponse(emptyList()))

        val result = recommendationService.getPersonalizedRecommendations()

        // 100 is seen -> filtered out; 102 is low rating -> filtered out. Only 101 remains.
        assertEquals(1, result.movies.size)
        assertEquals(101, result.movies[0].id)
        assertEquals("New Recommended Movie", result.movies[0].title)
    }

    @Test
    fun cacheMechanism_and_invalidateCache() = runTest {
        val watched = listOf(createWatchedItem(1, "Watched Movie", "movie", 9.0f))
        whenever(repository.getWatchedItemsSync()).thenReturn(watched)
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getMovieRecommendations(1)).thenReturn(listOf(createMovieResult(101, "Recommended", 7.5f)))
        whenever(repository.getPopularMovies()).thenReturn(MoviesResponse(emptyList()))
        whenever(repository.getPopularTvShows()).thenReturn(TvShowsResponse(emptyList()))

        // First call populates cache
        recommendationService.getPersonalizedRecommendations()
        // Second call should return cached
        recommendationService.getPersonalizedRecommendations()

        verify(repository, times(1)).getWatchedItemsSync()

        // Invalidate cache
        recommendationService.invalidateCache()

        // Third call should query repository again
        recommendationService.getPersonalizedRecommendations()
        verify(repository, times(2)).getWatchedItemsSync()
    }
}