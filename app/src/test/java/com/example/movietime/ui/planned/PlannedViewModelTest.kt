package com.example.movietime.ui.planned

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
class PlannedViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val repository: AppRepository = mock()

    private lateinit var viewModel: PlannedViewModel

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
        viewModel = PlannedViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPlannedContent_success_updatesLiveData() = runTest {
        val testItems = listOf(
            createWatchedItem(1, "Movie 1", "movie"),
            createWatchedItem(2, "Show 1", "tv")
        )
        whenever(repository.getPlannedItemsSync()).thenReturn(testItems)

        viewModel.loadPlannedContent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testItems, viewModel.plannedContent.value)
        assertFalse(viewModel.isLoading.value ?: true)
        assertNull(viewModel.errorMessage.value)
        assertEquals(1, viewModel.getPlannedMoviesCount())
        assertEquals(1, viewModel.getPlannedTvShowsCount())
        assertEquals(2, viewModel.getTotalPlannedCount())
    }

    @Test
    fun loadPlannedContent_failure_setsErrorMessage() = runTest {
        whenever(repository.getPlannedItemsSync()).thenThrow(RuntimeException("Network error"))

        viewModel.loadPlannedContent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<WatchedItem>(), viewModel.plannedContent.value)
        assertFalse(viewModel.isLoading.value ?: true)
        assertTrue(viewModel.errorMessage.value?.contains("Network error") == true)
    }

    @Test
    fun addToPlanned_callsRepositoryAndReloads() = runTest {
        val item = createWatchedItem(5, "New Planned", "movie")
        whenever(repository.getPlannedItemsSync()).thenReturn(listOf(item))

        viewModel.addToPlanned(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).addToPlanned(item)
        assertEquals(listOf(item), viewModel.plannedContent.value)
    }

    @Test
    fun removeFromPlanned_callsRepositoryAndReloads() = runTest {
        val item = createWatchedItem(5, "To Remove", "movie")
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())

        viewModel.removeFromPlanned(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromPlanned(5, "movie")
        assertEquals(emptyList<WatchedItem>(), viewModel.plannedContent.value)
    }

    @Test
    fun moveToWatched_removesFromPlannedAndAddsToWatched() = runTest {
        val item = createWatchedItem(10, "Move Me", "movie")
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())

        viewModel.moveToWatched(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromPlanned(10, "movie")
        verify(repository).addWatchedItem(item)
    }

    @Test
    fun moveToWatching_removesFromPlannedAndAddsToWatching() = runTest {
        val item = createWatchedItem(12, "Move to Watching", "tv")
        whenever(repository.getPlannedItemsSync()).thenReturn(emptyList())

        viewModel.moveToWatching(item)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).removeFromPlanned(12, "tv")
        verify(repository).addToWatching(item)
    }
}