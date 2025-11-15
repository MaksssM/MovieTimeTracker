package com.example.movietime.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentDateTime(): String {
        return dateTimeFormat.format(Date())
    }

    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    fun parseDateTime(dateTimeString: String): Date? {
        return try {
            dateTimeFormat.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTimeAgo(dateTimeString: String): String {
        val date = parseDateTime(dateTimeString) ?: return ""
        val now = Date()
        val diff = now.time - date.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val weeks = days / 7

        return when {
            minutes < 60 -> "${minutes}хв тому"
            hours < 24 -> "${hours}год тому"
            days < 7 -> "${days}д тому"
            else -> "${weeks}тиж тому"
        }
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "${mins} хв"
            mins == 0 -> "${hours} год"
            else -> "${hours} год ${mins} хв"
        }
    }
}
