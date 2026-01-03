package com.example.movietime.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.movietime.R
import com.example.movietime.ui.main.MainActivity
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    
    private const val CHANNEL_ID_EPISODES = "new_episodes_channel"
    private const val CHANNEL_ID_RELEASES = "new_releases_channel"
    private const val WORK_TAG_EPISODES = "check_new_episodes"
    private const val WORK_TAG_RELEASES = "check_new_releases"
    
    fun scheduleEpisodeNotifications(context: Context, enableNotifications: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (enableNotifications) {
            // Schedule to check every 6 hours
            val episodeCheckWork = PeriodicWorkRequestBuilder<EpisodeNotificationWorker>(
                6, TimeUnit.HOURS
            ).addTag(WORK_TAG_EPISODES).build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_TAG_EPISODES,
                ExistingPeriodicWorkPolicy.KEEP,
                episodeCheckWork
            )
        } else {
            workManager.cancelAllWorkByTag(WORK_TAG_EPISODES)
        }
    }
    
    fun scheduleReleaseNotifications(context: Context, enableNotifications: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (enableNotifications) {
            // Schedule to check daily
            val releaseCheckWork = PeriodicWorkRequestBuilder<ReleaseNotificationWorker>(
                1, TimeUnit.DAYS
            ).addTag(WORK_TAG_RELEASES).build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_TAG_RELEASES,
                ExistingPeriodicWorkPolicy.KEEP,
                releaseCheckWork
            )
        } else {
            workManager.cancelAllWorkByTag(WORK_TAG_RELEASES)
        }
    }
    
    fun createEpisodeNotification(context: Context, showTitle: String, episodeTitle: String, seasonNumber: Int, episodeNumber: Int) {
        createNotificationChannel(context, CHANNEL_ID_EPISODES, "Нові епізоди")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EPISODES)
            .setSmallIcon(R.drawable.ic_tv)
            .setContentTitle("$showTitle - S${seasonNumber}E${episodeNumber}")
            .setContentText(episodeTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(showTitle.hashCode(), notification)
    }
    
    fun createReleaseNotification(context: Context, title: String, releaseDate: String, mediaType: String) {
        createNotificationChannel(context, CHANNEL_ID_RELEASES, "Нові релізи")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val typeText = if (mediaType == "movie") "Фільм" else "Серіал"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_RELEASES)
            .setSmallIcon(R.drawable.ic_movie)
            .setContentTitle("$typeText: $title")
            .setContentText("Вийшов: $releaseDate")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(title.hashCode(), notification)
    }
    
    private fun createNotificationChannel(context: Context, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
