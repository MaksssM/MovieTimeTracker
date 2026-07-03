package com.example.movietime.util

import android.content.Context
import com.example.movietime.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private fun getDateTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    fun getCurrentDateTime(): String {
        return getDateTimeFormat().format(Date())
    }

    fun getCurrentDate(): String {
        return getDateFormat().format(Date())
    }

    fun parseDateTime(dateTimeString: String): Date? {
        return try {
            getDateTimeFormat().parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTimeAgo(context: Context, dateTimeString: String): String {
        val date = parseDateTime(dateTimeString) ?: return ""
        val now = Date()
        val diff = now.time - date.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val weeks = days / 7

        return when {
            minutes < 60 -> context.getString(R.string.time_ago_minutes, minutes)
            hours < 24 -> context.getString(R.string.time_ago_hours, hours)
            days < 7 -> context.getString(R.string.time_ago_days, days)
            else -> context.getString(R.string.time_ago_weeks, weeks)
        }
    }

    fun formatDuration(context: Context, minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> context.getString(R.string.time_format_minutes, mins)
            mins == 0 -> context.getString(R.string.time_format_hours, hours)
            else -> context.getString(R.string.time_format_hours_minutes, hours, mins)
        }
    }
}
