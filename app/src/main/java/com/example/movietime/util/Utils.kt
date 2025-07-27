package com.example.movietime.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movietime.R

fun formatMinutesToHoursAndMinutes(totalMinutes: Int?): String {
    if (totalMinutes == null || totalMinutes <= 0) {
        return "0ч 0м"
    }
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> "${days}д ${hours}ч ${minutes}м"
        hours > 0 -> "${hours}ч ${minutes}м"
        else -> "${minutes}м"
    }
}

fun ImageView.loadImage(posterPath: String?) {
    val fullUrl = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else null
    Glide.with(this.context)
        .load(fullUrl)
        .placeholder(R.drawable.ic_placeholder) // Мы создадим эту иконку
        .error(R.drawable.ic_placeholder)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}