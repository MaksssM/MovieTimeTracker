package com.example.movietime.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.movietime.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class EpisodeNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting episode check work")
        return try {
            val watchingShows = repository.getWatchingItemsForBackup() // Or dedicated optimized query
            Log.d(TAG, "Found ${watchingShows.size} watching shows")

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val prefs = applicationContext.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)

            for (show in watchingShows) {
                if (show.mediaType == "tv") {
                    try {
                        val details = repository.getTvShowDetails(show.id)
                        val nextEp = details.nextEpisodeToAir

                        if (nextEp != null) {
                            Log.d(TAG, "Show ${show.title}: Next ep ${nextEp.airDate}")
                            // Check if air date is today
                            if (nextEp.airDate == today) {
                                val notifKey = "notified_${show.id}_s${nextEp.seasonNumber}e${nextEp.episodeNumber}"
                                val alreadyNotified = prefs.getBoolean(notifKey, false)

                                if (!alreadyNotified) {
                                    NotificationHelper.createEpisodeNotification(
                                        applicationContext,
                                        show.title,
                                        nextEp.name ?: "Episode ${nextEp.episodeNumber}",
                                        nextEp.seasonNumber ?: 0,
                                        nextEp.episodeNumber ?: 0,
                                        details.posterPath
                                    )
                                    // Mark as notified
                                    prefs.edit().putBoolean(notifKey, true).apply()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking details for ${show.title}", e)
                        // Continue to next show
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in EpisodeNotificationWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "EpisodeNotifWorker"
    }
}
