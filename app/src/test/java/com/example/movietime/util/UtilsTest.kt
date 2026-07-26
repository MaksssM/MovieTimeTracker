package com.example.movietime.util

import android.content.Context
import com.example.movietime.data.db.WatchedItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class UtilsTest {

    @Mock
    lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock resource strings
        whenever(context.getString(eq(com.example.movietime.R.string.time_format_hours_minutes), any(), any())).thenAnswer { invocation ->
            val h = invocation.arguments[1] as Int
            val m = invocation.arguments[2] as Int
            "$h год $m хв"
        }
        whenever(context.getString(eq(com.example.movietime.R.string.time_format_hours), any())).thenAnswer { invocation ->
            val h = invocation.arguments[1] as Int
            "$h год"
        }
    }

    @Test
    fun formatMinutes_nullOrZero() {
        assertEquals("0 год 0 хв", Utils.formatMinutesToHoursAndMinutes(context, null))
        assertEquals("0 год 0 хв", Utils.formatMinutesToHoursAndMinutes(context, 0))
    }

    @Test
    fun formatMinutes_hoursAndMinutes() {
        assertEquals("2 год 30 хв", Utils.formatMinutesToHoursAndMinutes(context, 150))
        assertEquals("1 год 5 хв", Utils.formatMinutesToHoursAndMinutes(context, 65))
    }
    
    @Test
    fun formatMinutes_exactHours() {
        assertEquals("2 год", Utils.formatMinutesToHoursAndMinutes(context, 120))
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
