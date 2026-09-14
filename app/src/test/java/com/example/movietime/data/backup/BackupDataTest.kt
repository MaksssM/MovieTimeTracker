package com.example.movietime.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun backupData_serializationRoundTrip() {
        val watchedItem = WatchedItemData(
            id = 550,
            title = "Fight Club",
            posterPath = "/poster.jpg",
            releaseDate = "1999-10-15",
            runtime = 139,
            mediaType = "movie",
            overview = "An office worker...",
            voteAverage = 8.4,
            userRating = 10.0f,
            episodeRuntime = null,
            totalEpisodes = null,
            isOngoing = false,
            status = "Released",
            genreIds = "18,28",
            watchCount = 2,
            lastUpdated = 1700000000000L
        )

        val plannedItem = PlannedItemData(
            id = 101,
            title = "Oppenheimer",
            posterPath = "/opp.jpg",
            releaseDate = "2023-07-21",
            runtime = 180,
            mediaType = "movie",
            dateAdded = 1690000000000L
        )

        val watchingItem = WatchingItemData(
            id = 202,
            title = "Breaking Bad",
            posterPath = "/bb.jpg",
            releaseDate = "2008-01-20",
            runtime = 45,
            mediaType = "tv",
            dateAdded = 1680000000000L,
            currentEpisode = 5,
            currentSeason = 2
        )

        val backupData = BackupData(
            version = CURRENT_BACKUP_VERSION,
            createdAt = "2026-03-12 10:00:00",
            deviceName = "Pixel 8 Pro",
            appVersion = "1.0.0",
            watchedCount = 1,
            plannedCount = 1,
            watchingCount = 1,
            watched = listOf(watchedItem),
            planned = listOf(plannedItem),
            watching = listOf(watchingItem)
        )

        val encodedJson = json.encodeToString(backupData)
        assertNotNull(encodedJson)

        val decodedData = json.decodeFromString<BackupData>(encodedJson)

        assertEquals(CURRENT_BACKUP_VERSION, decodedData.version)
        assertEquals("2026-03-12 10:00:00", decodedData.createdAt)
        assertEquals("Pixel 8 Pro", decodedData.deviceName)
        assertEquals(1, decodedData.watched.size)
        assertEquals("Fight Club", decodedData.watched[0].title)
        assertEquals(2, decodedData.watched[0].watchCount)
        assertEquals(1, decodedData.planned.size)
        assertEquals("Oppenheimer", decodedData.planned[0].title)
        assertEquals(1, decodedData.watching.size)
        assertEquals("Breaking Bad", decodedData.watching[0].title)
        assertEquals(5, decodedData.watching[0].currentEpisode)
        assertEquals(2, decodedData.watching[0].currentSeason)
    }

    @Test
    fun backupMetadata_parsesHeaderOnly() {
        val jsonString = """
            {
                "version": 2,
                "createdAt": "2026-03-12",
                "deviceName": "Samsung S24",
                "appVersion": "2.1.0",
                "watchedCount": 42,
                "plannedCount": 10,
                "watchingCount": 3,
                "watched": [{"id":1,"title":"Ignored in Metadata"}]
            }
        """.trimIndent()

        val metadata = json.decodeFromString<BackupMetadata>(jsonString)

        assertEquals(2, metadata.version)
        assertEquals("2026-03-12", metadata.createdAt)
        assertEquals("Samsung S24", metadata.deviceName)
        assertEquals("2.1.0", metadata.appVersion)
        assertEquals(42, metadata.watchedCount)
        assertEquals(10, metadata.plannedCount)
        assertEquals(3, metadata.watchingCount)
    }
}