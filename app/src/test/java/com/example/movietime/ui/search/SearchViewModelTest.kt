package com.example.movietime.ui.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.model.Genre
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val repository: AppRepository = mock()
    private val api: TmdbApi = mock()
    private val languageManager: LanguageManager = mock()

    private lateinit var viewModel: SearchViewModel

    private fun createMovie(id: Int, title: String, rating: Float): MovieResult {
        return MovieResult(
            id = id,
            title = title,
            overview = null,
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            runtime = 120,
            voteAverage = rating,
            popularity = 100f
        )
    }

    private fun createTvShow(id: Int, name: String, rating: Float): TvShowResult {
        return TvShowResult(
            id = id,
            name = name,
            overview = null,
            posterPath = null,
            backdropPath = null,
            firstAirDate = null,
            lastAirDate = null,
            voteAverage = rating,
            popularity = 100f
        )
    }

    private fun createWatchedItem(id: Int, title: String, mediaType: String): WatchedItem {
        return WatchedItem(
            id = id,
            title = title,
            posterPath = null,
            releaseDate = null,
            runtime = 120,
            mediaType = mediaType,
            userRating = 8.0f,
            lastUpdated = System.currentTimeMillis()
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        viewModel = SearchViewModel(repository, api, languageManager)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun init_loadsGenresAndLibraryStatus() = runTest {
        val genres = listOf(Genre(1, "Action"), Genre(2, "Comedy"))
        whenever(repository.getAllGenres()).thenReturn(genres)
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(listOf(createWatchedItem(10, "Movie 10", "movie")))
        whenever(repository.getPlannedItemsSync()).thenReturn(listOf(createWatchedItem(20, "Movie 20", "movie")))
        whenever(repository.getWatchingItemsSync()).thenReturn(listOf(createWatchedItem(30, "Movie 30", "tv")))

        initViewModel()

        assertEquals(2, viewModel.availableGenres.value?.size)
        assertEquals("Action", viewModel.availableGenres.value?.get(0)?.name)

        val statusMap = viewModel.libraryStatusMap.value
        assertEquals("watched", statusMap["10_movie"])
        assertEquals("planned", statusMap["20_movie"])
        assertEquals("watching", statusMap["30_tv"])
    }

    @Test
    fun searchMulti_emptyQuery_clearsResults() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        initViewModel()

        viewModel.searchMulti("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.searchResult.value?.size)
    }

    @Test
    fun searchMulti_success_updatesSearchResultsAndHistory() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        val searchItems = listOf(
            createMovie(1, "Inception", 8.8f),
            createTvShow(2, "Breaking Bad", 9.5f)
        )
        whenever(repository.searchMultiLanguage(eq("Inception"), eq(1))).thenReturn(searchItems)

        initViewModel()

        viewModel.searchMulti("Inception")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.searchResult.value?.size)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals("Inception", viewModel.searchHistory.value?.firstOrNull())
    }

    @Test
    fun searchMulti_filterByType_moviesAndTvShows() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        val movie = createMovie(1, "Inception", 8.8f)
        val tvShow = createTvShow(2, "Breaking Bad", 9.5f)
        whenever(repository.searchMultiLanguage(eq("test"), eq(1))).thenReturn(listOf(movie, tvShow))

        initViewModel()

        viewModel.searchMulti("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Filter to MOVIES only
        viewModel.setFilterType(SearchViewModel.FilterType.MOVIES)
        assertEquals(1, viewModel.searchResult.value?.size)
        assertTrue(viewModel.searchResult.value?.first() is MovieResult)

        // Filter to TV_SHOWS only
        viewModel.setFilterType(SearchViewModel.FilterType.TV_SHOWS)
        assertEquals(1, viewModel.searchResult.value?.size)
        assertTrue(viewModel.searchResult.value?.first() is TvShowResult)

        // Reset to ALL
        viewModel.setFilterType(SearchViewModel.FilterType.ALL)
        assertEquals(2, viewModel.searchResult.value?.size)
    }
    @Before
    @Test
    fun searchMulti_filterByMinRating() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        val highRated = createMovie(1, "Great Movie", 9.0f)
        val lowRated = createMovie(2, "Mediocre Movie", 5.0f)
        whenever(repository.searchMultiLanguage(eq("movie"), eq(1))).thenReturn(listOf(highRated, lowRated))

        initViewModel()

        viewModel.searchMulti("movie")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setMinRating(8.0)
        assertEquals(1, viewModel.searchResult.value?.size)
        assertEquals("Great Movie", (viewModel.searchResult.value?.first() as MovieResult).title)
    }

    @Test
    fun searchMulti_error_setsErrorMessage() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        whenever(repository.searchMultiLanguage(any(), any())).thenThrow(RuntimeException("Network error"))

        initViewModel()

        viewModel.searchMulti("query")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.searchResult.value?.size)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Network error"))
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun searchHistory_limitsTo10AndRemovesItem() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())
        whenever(repository.searchMultiLanguage(any(), any())).thenReturn(emptyList())

        initViewModel()

        for (i in 1..12) {
            viewModel.searchMulti("Query $i")
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val history = viewModel.searchHistory.value
        assertEquals(10, history?.size)
        assertEquals("Query 12", history?.first())

        viewModel.removeHistoryItem("Query 12")
        assertEquals(9, viewModel.searchHistory.value?.size)
        assertFalse(viewModel.searchHistory.value!!.contains("Query 12"))

        viewModel.clearSearchHistory()
        assertEquals(0, viewModel.searchHistory.value?.size)
    }

    @Test
    fun genres_toggleAndClear() = runTest {
        whenever(repository.getAllGenres()).thenReturn(emptyList())
        whenever(repository.getAllSeenItemIds()).thenReturn(emptyList())
        whenever(repository.getWatchedItemsSync()).thenReturn(emptyList())
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        initViewModel()

        val action = Genre(1, "Action")
        viewModel.toggleGenre(action)
        assertEquals(1, viewModel.selectedGenres.value?.size)
        assertTrue(viewModel.isGenreSelected(action))

        // Toggle again to remove
        viewModel.toggleGenre(action)
        assertEquals(0, viewModel.selectedGenres.value?.size)
        assertFalse(viewModel.isGenreSelected(action))

        // Add back and clear
        viewModel.toggleGenre(action)
        viewModel.clearSelectedGenres()
        assertEquals(0, viewModel.selectedGenres.value?.size)
    }
}
