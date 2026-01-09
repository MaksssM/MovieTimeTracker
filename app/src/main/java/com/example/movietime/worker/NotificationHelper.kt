package com.example.movietime.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.movietime.R
import com.example.movietime.ui.main.MainActivity

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.movietime.worker.EpisodeNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationHelper {

    private const val CHANNEL_ID_EPISODES = "new_episodes_channel"
    private const val CHANNEL_ID_RELEASES = "new_releases_channel"
    private const val WORK_TAG_EPISODES = "check_new_episodes"

    fun scheduleEpisodeNotifications(context: Context, enableNotifications: Boolean) {
        val workManager = WorkManager.getInstance(context)

        if (enableNotifications) {
            // Check every 12 hours
            val episodeCheckWork = PeriodicWorkRequestBuilder<EpisodeNotificationWorker>(
                12, TimeUnit.HOURS
            ).addTag(WORK_TAG_EPISODES).build()

            workManager.enqueueUniquePeriodicWork(
                WORK_TAG_EPISODES,
                ExistingPeriodicWorkPolicy.UPDATE, // Update if settings changed or just keep
                episodeCheckWork
            )
        } else {
            workManager.cancelAllWorkByTag(WORK_TAG_EPISODES)
        }
    }

    fun createEpisodeNotification(context: Context, showTitle: String, episodeTitle: String, seasonNumber: Int, episodeNumber: Int, posterPath: String?) {
        createNotificationChannel(context, CHANNEL_ID_EPISODES, context.getString(R.string.notif_channel_episodes))

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_fragment", "tv_details")
            putExtra("show_title", showTitle)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, showTitle.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EPISODES)
            .setSmallIcon(R.drawable.ic_tv)
            .setContentTitle("$showTitle")
            .setContentText("Нова серія: S${seasonNumber}E${episodeNumber} - $episodeTitle")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(showTitle.hashCode(), notification)
    }

    fun createReleaseNotification(context: Context, title: String, releaseDate: String, mediaType: String) {
        createNotificationChannel(context, CHANNEL_ID_RELEASES, "Нові релізи") // Consider moving string to strings.xml

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, title.hashCode(), intent,
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
            ).apply {
                description = "Sporadic notifications about new episodes"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
