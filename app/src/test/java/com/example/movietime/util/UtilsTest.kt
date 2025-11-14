package com.example.movietime.util

import com.example.movietime.data.db.WatchedItem
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun formatMinutes_nullOrZero() {
        assertEquals("0 год 0 хв", Utils.formatMinutesToHoursAndMinutes(null))
        assertEquals("0 год 0 хв", Utils.formatMinutesToHoursAndMinutes(0))
    }

    @Test
    fun formatMinutes_hoursAndMinutes() {
        assertEquals("2 год 30 хв", Utils.formatMinutesToHoursAndMinutes(150))
        assertEquals("1 год 5 хв", Utils.formatMinutesToHoursAndMinutes(65))
    }

    @Test
    fun sumRuntime_ofWatchedItems() {
        val items = listOf(
            WatchedItem(id = 1, title = "A", posterPath = null, releaseDate = null, runtime = 120, mediaType = "movie"),
            WatchedItem(id = 2, title = "B", posterPath = null, releaseDate = null, runtime = 45, mediaType = "movie"),
            WatchedItem(id = 3, title = "C", posterPath = null, releaseDate = null, runtime = null, mediaType = "tv")
        )

        val sum = items.sumOf { it.runtime ?: 0 }
        assertEquals(165, sum)
    }
}

