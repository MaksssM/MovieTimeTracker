package com.example.movietime.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.PlannedItem
import com.example.movietime.data.db.WatchingItem
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: String = "",
    val watched: List<WatchedItemData> = emptyList(),
    val planned: List<PlannedItemData> = emptyList(),
    val watching: List<WatchingItemData> = emptyList()
)

@Serializable
data class WatchedItemData(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val overview: String?,
    val voteAverage: Double?,
    val userRating: Float?,
    val episodeRuntime: Int?,
    val totalEpisodes: Int?,
    val isOngoing: Boolean = false,
    val status: String?,
    val genreIds: String?,
    val watchCount: Int = 1,
    val lastUpdated: Long? = null
)

@Serializable
data class PlannedItemData(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Serializable
data class WatchingItemData(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int?,
    val mediaType: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val currentEpisode: Int? = null,
    val currentSeason: Int? = null
)

class BackupManager(
    private val context: Context,
    private val repository: AppRepository
) {
    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_DIR = "backups"
        private const val JSON = "json"
    }

    private val backupDir = File(context.filesDir, BACKUP_DIR)

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "backup_$timestamp.json"
            val file = File(backupDir, fileName)

            // Завантажити дані (використовуємо спеціальні suspend методи для backup)
            val watchedItems = repository.getWatchedItemsForBackup()
            val plannedItems = repository.getPlannedItemsForBackup()
            val watchingItems = repository.getWatchingItemsForBackup()

            // Конвертувати в data classes
            val watchedData = watchedItems.map { item ->
                WatchedItemData(
                    id = item.id,
                    title = item.title,
                    posterPath = item.posterPath,
                    releaseDate = item.releaseDate,
                    runtime = item.runtime,
                    mediaType = item.mediaType,
                    overview = item.overview,
                    voteAverage = item.voteAverage,
                    userRating = item.userRating,
                    episodeRuntime = item.episodeRuntime,
                    totalEpisodes = item.totalEpisodes,
                    isOngoing = item.isOngoing,
                    status = item.status,
                    genreIds = item.genreIds,
                    watchCount = item.watchCount,
                    lastUpdated = item.lastUpdated
                )
            }

            val plannedData = plannedItems.map { plannedItem ->
                PlannedItemData(
                    id = plannedItem.id,
                    title = plannedItem.title,
                    posterPath = plannedItem.posterPath,
                    releaseDate = plannedItem.releaseDate,
                    runtime = plannedItem.runtime,
                    mediaType = plannedItem.mediaType,
                    dateAdded = plannedItem.dateAdded
                )
            }

            val watchingData = watchingItems.map { watchingItem ->
                WatchingItemData(
                    id = watchingItem.id,
                    title = watchingItem.title,
                    posterPath = watchingItem.posterPath,
                    releaseDate = watchingItem.releaseDate,
                    runtime = watchingItem.runtime,
                    mediaType = watchingItem.mediaType,
                    dateAdded = watchingItem.dateAdded,
                    currentEpisode = watchingItem.currentEpisode,
                    currentSeason = watchingItem.currentSeason
                )
            }

            val backup = BackupData(
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                watched = watchedData,
                planned = plannedData,
                watching = watchingData
            )

            // Записати JSON
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(BackupData.serializer(), backup)
            file.writeText(jsonString)

            Log.d(TAG, "Backup created: ${file.absolutePath}")
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            Result.failure(e)
        }
    }

    suspend fun createBackupToUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Завантажити дані
            val watchedItems = repository.getWatchedItemsForBackup()
            val plannedItems = repository.getPlannedItemsForBackup()
            val watchingItems = repository.getWatchingItemsForBackup()

            // Конвертувати в data classes
            val watchedData = watchedItems.map { item ->
                WatchedItemData(
                    id = item.id,
                    title = item.title,
                    posterPath = item.posterPath,
                    releaseDate = item.releaseDate,
                    runtime = item.runtime,
                    mediaType = item.mediaType,
                    overview = item.overview,
                    voteAverage = item.voteAverage,
                    userRating = item.userRating,
                    episodeRuntime = item.episodeRuntime,
                    totalEpisodes = item.totalEpisodes,
                    isOngoing = item.isOngoing,
                    status = item.status,
                    genreIds = item.genreIds,
                    watchCount = item.watchCount,
                    lastUpdated = item.lastUpdated
                )
            }

            val plannedData = plannedItems.map { plannedItem ->
                PlannedItemData(
                    id = plannedItem.id,
                    title = plannedItem.title,
                    posterPath = plannedItem.posterPath,
                    releaseDate = plannedItem.releaseDate,
                    runtime = plannedItem.runtime,
                    mediaType = plannedItem.mediaType,
                    dateAdded = plannedItem.dateAdded
                )
            }

            val watchingData = watchingItems.map { watchingItem ->
                WatchingItemData(
                    id = watchingItem.id,
                    title = watchingItem.title,
                    posterPath = watchingItem.posterPath,
                    releaseDate = watchingItem.releaseDate,
                    runtime = watchingItem.runtime,
                    mediaType = watchingItem.mediaType,
                    dateAdded = watchingItem.dateAdded,
                    currentEpisode = watchingItem.currentEpisode,
                    currentSeason = watchingItem.currentSeason
                )
            }

            val backup = BackupData(
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                watched = watchedData,
                planned = plannedData,
                watching = watchingData
            )

            // Записати JSON в URI
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(BackupData.serializer(), backup)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Log.d(TAG, "Backup created to URI: $uri")
            Result.success(uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup to URI", e)
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }

            val jsonString = file.readText()
            val json = Json
            val backup = json.decodeFromString(BackupData.serializer(), jsonString)

            // Видалити старі дані
            repository.deleteAllWatched()
            repository.deleteAllPlanned()
            repository.deleteAllWatching()

            // Додати нові дані
            backup.watched.forEach { data ->
                val item = WatchedItem(
                    id = data.id,
                    title = data.title,
                    posterPath = data.posterPath,
                    releaseDate = data.releaseDate,
                    runtime = data.runtime,
                    mediaType = data.mediaType,
                    overview = data.overview,
                    voteAverage = data.voteAverage,
                    userRating = data.userRating,
                    episodeRuntime = data.episodeRuntime,
                    totalEpisodes = data.totalEpisodes,
                    isOngoing = data.isOngoing,
                    status = data.status,
                    genreIds = data.genreIds,
                    watchCount = data.watchCount,
                    lastUpdated = data.lastUpdated
                )
                repository.addWatchedItem(item)
            }

            backup.planned.forEach { data ->
                repository.insertPlannedDirect(PlannedItem(
                    id = data.id,
                    title = data.title,
                    posterPath = data.posterPath,
                    releaseDate = data.releaseDate,
                    runtime = data.runtime,
                    mediaType = data.mediaType,
                    dateAdded = data.dateAdded
                ))
            }

            backup.watching.forEach { data ->
                repository.insertWatchingDirect(WatchingItem(
                    id = data.id,
                    title = data.title,
                    posterPath = data.posterPath,
                    releaseDate = data.releaseDate,
                    runtime = data.runtime,
                    mediaType = data.mediaType,
                    dateAdded = data.dateAdded,
                    currentEpisode = data.currentEpisode,
                    currentSeason = data.currentSeason
                ))
            }

            Log.d(TAG, "Backup restored successfully")
            Result.success("Backup restored successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            Result.failure(e)
        }
    }

    fun getBackupList(): List<BackupFile> {
        return backupDir.listFiles { file ->
            file.extension == JSON
        }?.map { file ->
            BackupFile(
                name = file.name,
                path = file.absolutePath,
                sizeKb = file.length() / 1024,
                dateModified = file.lastModified()
            )
        }?.sortedByDescending { it.dateModified } ?: emptyList()
    }

    suspend fun deleteBackup(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.delete()) {
                Result.success("Backup deleted")
            } else {
                Result.failure(Exception("Failed to delete backup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBackupsDirPath(): String = backupDir.absolutePath
}

data class BackupFile(
    val name: String,
    val path: String,
    val sizeKb: Long,
    val dateModified: Long
)
