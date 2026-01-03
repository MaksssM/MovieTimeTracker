package com.example.movietime.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.movietime.data.db.PlannedItem
import com.example.movietime.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class ReleaseNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get all planned items
            val plannedItems = repository.getPlannedItemsForBackup()
            
            // Check which items are being released today or soon
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            for (item in plannedItems) {
                val releaseDate = item.releaseDate
                if (releaseDate != null) {
                    try {
                        val releaseDateObj = dateFormat.parse(releaseDate)
                        val todayObj = dateFormat.parse(today)
                        
                        // Notify if release date is today
                        if (releaseDateObj == todayObj) {
                            NotificationScheduler.createReleaseNotification(
                                applicationContext,
                                item.title,
                                releaseDate,
                                item.mediaType
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("ReleaseNotificationWorker", "Could not parse date: $releaseDate", e)
                    }
                }
            }
            
            Log.d("ReleaseNotificationWorker", "Checked ${plannedItems.size} items for releases")
            Result.success()
        } catch (e: Exception) {
            Log.e("ReleaseNotificationWorker", "Error checking for releases", e)
            Result.retry()
        }
    }
}
