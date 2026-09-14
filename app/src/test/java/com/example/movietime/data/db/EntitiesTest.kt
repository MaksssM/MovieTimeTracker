package com.example.movietime.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitiesTest {

    @Test
    fun watchedItem_defaultValues() {
        val item = WatchedItem(
            id = 1,
            title = "Test Movie",
            posterPath = null,
            releaseDate = null,
            runtime = 120,
            mediaType = "movie"
        )

        assertEquals(1, item.id)
        assertEquals("Test Movie", item.title)
        assertEquals(120, item.runtime)
        assertEquals("movie", item.mediaType)
        assertFalse(item.isOngoing)
        assertEquals(1, item.watchCount)
        assertNull(item.overview)
        assertNull(item.voteAverage)
        assertNull(item.userRating)
        assertNull(item.genreIds)
    }

    @Test
    fun plannedItem_properties() {
        val item = PlannedItem(
            id = 10,
            title = "Dune 2",
            posterPath = "/dune.jpg",
            releaseDate = "2024-03-01",
            runtime = 166,
            mediaType = "movie"
        )

        assertEquals(10, item.id)
        assertEquals("Dune 2", item.title)
        assertEquals("/dune.jpg", item.posterPath)
        assertEquals("2024-03-01", item.releaseDate)
        assertEquals(166, item.runtime)
        assertEquals("movie", item.mediaType)
        assertTrue(item.dateAdded > 0)
    }

    @Test
    fun userCollection_defaultValues() {
        val collection = UserCollection(
            name = "Favorites"
        )

        assertEquals(0L, collection.id)
        assertEquals("Favorites", collection.name)
        assertNull(collection.description)
        assertNull(collection.coverImagePath)
        assertNull(collection.emoji)
        assertNull(collection.color)
        assertTrue(collection.createdAt > 0)
        assertTrue(collection.updatedAt > 0)
    }

    @Test
    fun collectionItem_properties() {
        val item = CollectionItem(
            collectionId = 5L,
            itemId = 100,
            mediaType = "tv",
            title = "Arcane",
            posterPath = "/arcane.jpg"
        )

        assertEquals(5L, item.collectionId)
        assertEquals(100, item.itemId)
        assertEquals("tv", item.mediaType)
        assertEquals("Arcane", item.title)
        assertEquals("/arcane.jpg", item.posterPath)
        assertTrue(item.addedAt > 0)
    }

    @Test
    fun rewatchEntry_defaultValues() {
        val rewatch = RewatchEntry(
            itemId = 550,
            mediaType = "movie",
            title = "Fight Club",
            userRating = 10.0f
        )

        assertEquals(0L, rewatch.id)
        assertEquals(550, rewatch.itemId)
        assertEquals("movie", rewatch.mediaType)
        assertEquals("Fight Club", rewatch.title)
        assertEquals(10.0f, rewatch.userRating ?: 0f, 0.001f)
        assertNull(rewatch.notes)
        assertNull(rewatch.watchTimeMinutes)
        assertTrue(rewatch.watchedAt > 0)
    }

    @Test
    fun followedPerson_defaultValues() {
        val person = FollowedPerson(
            personId = 1234,
            name = "Denis Villeneuve",
            knownForDepartment = "Directing"
        )

        assertEquals(1234, person.personId)
        assertEquals("Denis Villeneuve", person.name)
        assertEquals("Directing", person.knownForDepartment)
        assertTrue(person.notificationsEnabled)
        assertTrue(person.followedAt > 0)
    }

    @Test
    fun yearlyStats_defaultValues() {
        val stats = YearlyStats(year = 2026)

        assertEquals(2026, stats.year)
        assertEquals(0, stats.totalMovies)
        assertEquals(0, stats.totalTvEpisodes)
        assertEquals(0L, stats.totalWatchTimeMinutes)
        assertEquals(0, stats.mostRewatchedCount)
        assertEquals(0, stats.uniqueGenresCount)
        assertNull(stats.favoriteGenreName)
        assertNull(stats.topRatedItemTitle)
        assertNull(stats.monthlyBreakdown)
    }
}