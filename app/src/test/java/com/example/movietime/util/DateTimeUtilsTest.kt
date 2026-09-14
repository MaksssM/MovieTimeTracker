package com.example.movietime.util

import android.content.Context
import com.example.movietime.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class DateTimeUtilsTest {

    @Mock
    lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(context.getString(eq(R.string.time_format_minutes), any())).thenAnswer { invocation ->
            val m = invocation.arguments[1] as Int
            "$m хв"
        }
        whenever(context.getString(eq(R.string.time_format_hours), any())).thenAnswer { invocation ->
            val h = invocation.arguments[1] as Int
            "$h год"
        }
        whenever(context.getString(eq(R.string.time_format_hours_minutes), any(), any())).thenAnswer { invocation ->
            val h = invocation.arguments[1] as Int
            val m = invocation.arguments[2] as Int
            "$h год $m хв"
        }
        whenever(context.getString(eq(R.string.time_ago_minutes), any())).thenAnswer { invocation ->
            val m = invocation.arguments[1] as Long
            "$m хв тому"
        }
        whenever(context.getString(eq(R.string.time_ago_hours), any())).thenAnswer { invocation ->
            val h = invocation.arguments[1] as Long
            "$h год тому"
        }
        whenever(context.getString(eq(R.string.time_ago_days), any())).thenAnswer { invocation ->
            val d = invocation.arguments[1] as Long
            "$d дн тому"
        }
        whenever(context.getString(eq(R.string.time_ago_weeks), any())).thenAnswer { invocation ->
            val w = invocation.arguments[1] as Long
            "$w тиж тому"
        }
    }

    @Test
    fun parseDateTime_validFormat_returnsDate() {
        val result = DateTimeUtils.parseDateTime("2026-05-20 14:30:00")
        assertNotNull(result)
    }

    @Test
    fun parseDateTime_invalidFormat_returnsNull() {
        val result = DateTimeUtils.parseDateTime("invalid-date-format")
        assertNull(result)
    }

    @Test
    fun parseDateTime_emptyString_returnsNull() {
        val result = DateTimeUtils.parseDateTime("")
        assertNull(result)
    }

    @Test
    fun formatDuration_minutesOnly() {
        val formatted = DateTimeUtils.formatDuration(context, 45)
        assertEquals("45 хв", formatted)
    }

    @Test
    fun formatDuration_exactHours() {
        val formatted = DateTimeUtils.formatDuration(context, 120)
        assertEquals("2 год", formatted)
    }

    @Test
    fun formatDuration_hoursAndMinutes() {
        val formatted = DateTimeUtils.formatDuration(context, 135)
        assertEquals("2 год 15 хв", formatted)
    }

    @Test
    fun getCurrentDate_returnsNonEmptyString() {
        val date = DateTimeUtils.getCurrentDate()
        assertNotNull(date)
        assertEquals(10, date.length) // "yyyy-MM-dd"
    }

    @Test
    fun getCurrentDateTime_returnsNonEmptyString() {
        val dateTime = DateTimeUtils.getCurrentDateTime()
        assertNotNull(dateTime)
        assertEquals(19, dateTime.length) // "yyyy-MM-dd HH:mm:ss"
    }
}