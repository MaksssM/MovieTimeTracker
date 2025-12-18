package com.example.movietime.service

import android.util.Log
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервіс для оновлення серіалів що ще виходять
 */
@Singleton
class TvShowUpdateService @Inject constructor(
    private val repository: AppRepository
) {
    companion object {
        private const val TAG = "TvShowUpdateService"
        private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 години
    }

    /**
     * Оновлює всі серіали що ще виходять
     */
    suspend fun updateOngoingTvShows(): UpdateResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting update of ongoing TV shows")

        try {
            val watchedItems = repository.getAllWatchedSync()
            val ongoingTvShows = watchedItems.filter {
                it.mediaType == "tv" && it.isOngoing
            }

            Log.d(TAG, "Found ${ongoingTvShows.size} ongoing TV shows to update")

            var updatedCount = 0
            var errorCount = 0

            for (tvShow in ongoingTvShows) {
                try {
                    val shouldUpdate = shouldUpdateItem(tvShow)
                    if (shouldUpdate) {
                        val updated = updateTvShowItem(tvShow)
                        if (updated) {
                            updatedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update TV show ${tvShow.id}: ${e.message}")
                    errorCount++
                }
            }

            Log.d(TAG, "Update completed: $updatedCount updated, $errorCount errors")
            return@withContext UpdateResult(updatedCount, errorCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update ongoing TV shows: ${e.message}", e)
            return@withContext UpdateResult(0, 1)
        }
    }

    /**
     * Перевіряє чи потрібно оновлювати елемент
     */
    private fun shouldUpdateItem(item: WatchedItem): Boolean {
        val lastUpdated = item.lastUpdated ?: 0
        val now = System.currentTimeMillis()
        return (now - lastUpdated) > UPDATE_INTERVAL_MS
    }

    /**
     * Оновлює конкретний серіал
     */
    private suspend fun updateTvShowItem(oldItem: WatchedItem): Boolean {
        return try {
            Log.d(TAG, "Updating TV show: ${oldItem.title} (ID: ${oldItem.id})")

            // Отримуємо свіжі дані з API
            val tvShowDetails = repository.getTvShowDetails(oldItem.id)
            val runtimeInfo = Utils.autoComputeTvShowRuntime(tvShowDetails)

            // Перевіряємо чи змінились дані
            val hasChanges = checkForChanges(oldItem, tvShowDetails, runtimeInfo)

            if (hasChanges) {
                // Оновлюємо елемент зі збереженням користувацьких даних
                val updatedItem = oldItem.copy(
                    title = tvShowDetails.name ?: oldItem.title,
                    posterPath = tvShowDetails.posterPath ?: oldItem.posterPath,
                    overview = tvShowDetails.overview ?: oldItem.overview,
                    // Оновлюємо TV-специфічні дані
                    totalEpisodes = runtimeInfo.episodes,
                    isOngoing = runtimeInfo.isOngoing,
                    status = tvShowDetails.status,
                    lastUpdated = System.currentTimeMillis(),
                    // Перераховуємо загальний час тільки якщо використовувались автоматичні дані
                    runtime = if (wasUsingAutoData(oldItem, runtimeInfo)) {
                        runtimeInfo.totalMinutes
                    } else {
                        oldItem.runtime // Зберігаємо користувацькі дані
                    }
                )

                repository.addWatchedItem(updatedItem) // Це оновить існуючий запис
                Log.d(TAG, "Successfully updated TV show: ${oldItem.title}")
                return true
            } else {
                // Просто оновлюємо час останнього оновлення
                val updatedItem = oldItem.copy(lastUpdated = System.currentTimeMillis())
                repository.addWatchedItem(updatedItem)
                Log.d(TAG, "No changes for TV show: ${oldItem.title}")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating TV show ${oldItem.id}: ${e.message}", e)
            false
        }
    }

    /**
     * Перевіряє чи були зміни в даних серіалу
     */
    private fun checkForChanges(
        oldItem: WatchedItem,
        newData: com.example.movietime.data.model.TvShowResult,
        runtimeInfo: Utils.TvShowRuntimeInfo
    ): Boolean {
        return oldItem.totalEpisodes != runtimeInfo.episodes ||
               oldItem.isOngoing != runtimeInfo.isOngoing ||
               oldItem.status != newData.status ||
               oldItem.title != (newData.name ?: oldItem.title)
    }

    /**
     * Перевіряє чи використовувались автоматичні дані для обчислення часу
     */
    private fun wasUsingAutoData(item: WatchedItem, runtimeInfo: Utils.TvShowRuntimeInfo): Boolean {
        val expectedTotalTime = (item.episodeRuntime ?: 0) * (item.totalEpisodes ?: 0)
        return item.runtime == expectedTotalTime || runtimeInfo.isEstimated
    }

    data class UpdateResult(
        val updatedCount: Int,
        val errorCount: Int
    ) {
        val isSuccess: Boolean get() = errorCount == 0
        val hasUpdates: Boolean get() = updatedCount > 0
    }
}
