package com.example.movietime.ui.watching

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WatchingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val repository: AppRepository = mock()

    private lateinit var viewModel: WatchingViewModel

    private fun createWatchedItem(id: Int, title: String, mediaType: String): WatchedItem {
        return WatchedItem(
            id = id,
            title = title,
            posterPath = null,
            releaseDate = null,
            runtime = 120,
            mediaType = mediaType
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WatchingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadWatchingContent_success_updatesLiveData() = runTest {
        val testItems = listOf(
            createWatchedItem(10, "Ongoing Show", "tv"),
            createWatchedItem(20, "Currently Watching Movie", "movie")
        )
        whenever(repository.getWatchingItemsSync()).thenReturn(testItems)

        viewModel.loadWatchingContent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testItems, viewModel.watchingContent.value)
        assertFalse(viewModel.isLoading.value ?: true)
        assertNull(viewModel.errorMessage.value)
        assertEquals(1, viewModel.getWatchingMoviesCount())
        assertEquals(1, viewModel.getWatchingTvShowsCount())
        assertEquals(2, viewModel.getTotalWatchingCount())
    }

    @Test
    fun loadWatchingContent_error_setsErrorMessage() = runTest {
        whenever(repository.getWatchingItemsSync()).thenThrow(RuntimeException("DB failure"))

        viewModel.loadWatchingContent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<WatchedItem>(), viewModel.watchingContent.value)
        assertFalse(viewModel.isLoading.value ?: true)
        assertTrue(viewModel.errorMessage.value?.contains("DB failure") == true)
    }

    @Test
    fun addToWatching_callsRepositoryAndReloads() = runTest {
        val item = createWatchedItem(30, "Severance", "tv")
        whenever(repository.getWatchingItemsSync()).thenReturn(listOf(item))

        viewModel.addToWatching(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).addToWatching(item)
        assertEquals(listOf(item), viewModel.watchingContent.value)
    }

    @Test
    fun removeFromWatching_callsRepositoryAndReloads() = runTest {
        val item = createWatchedItem(30, "Severance", "tv")
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        viewModel.removeFromWatching(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromWatching(30, "tv")
        assertEquals(emptyList<WatchedItem>(), viewModel.watchingContent.value)
    }

    @Test
    fun moveToWatched_removesFromWatchingAndAddsToWatched() = runTest {
        val item = createWatchedItem(30, "Severance", "tv")
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        viewModel.moveToWatched(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromWatching(30, "tv")
        verify(repository).addWatchedItem(item)
    }

    @Test
    fun moveToPlanned_removesFromWatchingAndAddsToPlanned() = runTest {
        val item = createWatchedItem(30, "Severance", "tv")
        whenever(repository.getWatchingItemsSync()).thenReturn(emptyList())

        viewModel.moveToPlanned(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromWatching(30, "tv")
        verify(repository).addToPlanned(item)
    }
}