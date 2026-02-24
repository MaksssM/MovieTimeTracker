package com.example.movietime.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.movietime.BuildConfig
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

const val CURRENT_BACKUP_VERSION = 2
const val MAX_AUTO_BACKUPS = 3

@Serializable
data class BackupData(
    val version: Int = CURRENT_BACKUP_VERSION,
    val createdAt: String = "",
    val deviceName: String = "",
    val appVersion: String = "",
    val watchedCount: Int = 0,
    val plannedCount: Int = 0,
    val watchingCount: Int = 0,
    val watched: List<WatchedItemData> = emptyList(),
    val planned: List<PlannedItemData> = emptyList(),
    val watching: List<WatchingItemData> = emptyList()
)

/** Lightweight class to parse only the header fields from a backup file. */
@Serializable
data class BackupMetadata(
    val version: Int = 1,
    val createdAt: String = "",
    val deviceName: String = "",
    val appVersion: String = "",
    val watchedCount: Int = 0,
    val plannedCount: Int = 0,
    val watchingCount: Int = 0
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
        const val BACKUP_DIR = "backups"
        private const val JSON_EXT = "json"
        const val AUTO_BACKUP_PREFIX = "auto_"

        val jsonConfig = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    private val backupDir = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }

    // ── Core Helpers ─────────────────────────────────────────────────────────

    private suspend fun buildBackupData(): BackupData {
        val watchedItems = repository.getWatchedItemsForBackup()
        val plannedItems = repository.getPlannedItemsForBackup()
        val watchingItems = repository.getWatchingItemsForBackup()

        val watchedData = watchedItems.map { item ->
            WatchedItemData(
                id = item.id, title = item.title, posterPath = item.posterPath,
                releaseDate = item.releaseDate, runtime = item.runtime, mediaType = item.mediaType,
                overview = item.overview, voteAverage = item.voteAverage, userRating = item.userRating,
                episodeRuntime = item.episodeRuntime, totalEpisodes = item.totalEpisodes,
                isOngoing = item.isOngoing, status = item.status, genreIds = item.genreIds,
                watchCount = item.watchCount, lastUpdated = item.lastUpdated
            )
        }
        val plannedData = plannedItems.map { item ->
            PlannedItemData(
                id = item.id, title = item.title, posterPath = item.posterPath,
                releaseDate = item.releaseDate, runtime = item.runtime,
                mediaType = item.mediaType, dateAdded = item.dateAdded
            )
        }
        val watchingData = watchingItems.map { item ->
            WatchingItemData(
                id = item.id, title = item.title, posterPath = item.posterPath,
                releaseDate = item.releaseDate, runtime = item.runtime,
                mediaType = item.mediaType, dateAdded = item.dateAdded,
                currentEpisode = item.currentEpisode, currentSeason = item.currentSeason
            )
        }

        return BackupData(
            version = CURRENT_BACKUP_VERSION,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            deviceName = Build.MODEL,
            appVersion = BuildConfig.VERSION_NAME,
            watchedCount = watchedData.size,
            plannedCount = plannedData.size,
            watchingCount = watchingData.size,
            watched = watchedData,
            planned = plannedData,
            watching = watchingData
        )
    }

    private suspend fun applyBackupData(backup: BackupData, merge: Boolean) {
        if (!merge) {
            repository.deleteAllWatched()
            repository.deleteAllPlanned()
            repository.deleteAllWatching()
            backup.watched.forEach { repository.addWatchedItem(watchedItemFromData(it)) }
            backup.planned.forEach { repository.insertPlannedDirect(plannedItemFromData(it)) }
            backup.watching.forEach { repository.insertWatchingDirect(watchingItemFromData(it)) }
        } else {
            val existingWatched = repository.getWatchedItemIds()
            val existingPlanned = repository.getPlannedItemIds()
            val existingWatching = repository.getWatchingItemIds()
            backup.watched
                .filter { (it.id to it.mediaType) !in existingWatched }
                .forEach { repository.addWatchedItem(watchedItemFromData(it)) }
            backup.planned
                .filter { (it.id to it.mediaType) !in existingPlanned }
                .forEach { repository.insertPlannedDirect(plannedItemFromData(it)) }
            backup.watching
                .filter { (it.id to it.mediaType) !in existingWatching }
                .forEach { repository.insertWatchingDirect(watchingItemFromData(it)) }
        }
    }

    // ── Item Converters ──────────────────────────────────────────────────────

    private fun watchedItemFromData(data: WatchedItemData) = WatchedItem(
        id = data.id, title = data.title, posterPath = data.posterPath,
        releaseDate = data.releaseDate, runtime = data.runtime, mediaType = data.mediaType,
        overview = data.overview, voteAverage = data.voteAverage, userRating = data.userRating,
        episodeRuntime = data.episodeRuntime, totalEpisodes = data.totalEpisodes,
        isOngoing = data.isOngoing, status = data.status, genreIds = data.genreIds,
        watchCount = data.watchCount, lastUpdated = data.lastUpdated
    )

    private fun plannedItemFromData(data: PlannedItemData) = PlannedItem(
        id = data.id, title = data.title, posterPath = data.posterPath,
        releaseDate = data.releaseDate, runtime = data.runtime,
        mediaType = data.mediaType, dateAdded = data.dateAdded
    )

    private fun watchingItemFromData(data: WatchingItemData) = WatchingItem(
        id = data.id, title = data.title, posterPath = data.posterPath,
        releaseDate = data.releaseDate, runtime = data.runtime,
        mediaType = data.mediaType, dateAdded = data.dateAdded,
        currentEpisode = data.currentEpisode, currentSeason = data.currentSeason
    )

    // ── Auto-backup cleanup ──────────────────────────────────────────────────

    private fun cleanupAutoBackups() {
        backupDir.listFiles { f -> f.isFile && f.name.startsWith(AUTO_BACKUP_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_AUTO_BACKUPS)
            ?.forEach { it.delete() }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val file = File(backupDir, "backup_$ts.json")
            val backup = buildBackupData()
            file.writeText(jsonConfig.encodeToString(BackupData.serializer(), backup))
            Log.d(TAG, "Backup created: ${file.name}")
            file.absolutePath
        }
    }

    suspend fun createBackupToUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = buildBackupData()
            val jsonString = jsonConfig.encodeToString(BackupData.serializer(), backup)
            context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                ?: error("Failed to open output stream")
            Log.d(TAG, "Backup exported to URI: $uri")
            uri.toString()
        }
    }

    /** Creates an automatic safety backup. Keeps only [MAX_AUTO_BACKUPS] auto-backups. */
    suspend fun createAutoBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val file = File(backupDir, "${AUTO_BACKUP_PREFIX}${ts}.json")
            val backup = buildBackupData()
            file.writeText(jsonConfig.encodeToString(BackupData.serializer(), backup))
            cleanupAutoBackups()
            Log.d(TAG, "Auto-backup created: ${file.name}")
            file.absolutePath
        }
    }

    suspend fun restoreBackup(filePath: String, merge: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            require(file.exists()) { "Backup file not found" }
            val backup = jsonConfig.decodeFromString(BackupData.serializer(), file.readText())
            applyBackupData(backup, merge)
            Log.d(TAG, "Backup restored (merge=$merge): ${file.name}")
            "OK"
        }
    }

    suspend fun restoreBackupFromUri(context: Context, uri: Uri, merge: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = context.contentResolver.openInputStream(uri)
                ?.use { it.bufferedReader().readText() }
                ?: error("Failed to read backup file")
            val backup = jsonConfig.decodeFromString(BackupData.serializer(), jsonString)
            applyBackupData(backup, merge)
            Log.d(TAG, "Backup restored from URI (merge=$merge)")
            "OK"
        }
    }

    suspend fun getBackupListAsync(): List<BackupFile> = withContext(Dispatchers.IO) {
        backupDir.listFiles { f -> f.isFile && f.extension == JSON_EXT }
            ?.mapNotNull { file ->
                try {
                    val meta = jsonConfig.decodeFromString(BackupMetadata.serializer(), file.readText())
                    BackupFile(
                        name = file.name,
                        path = file.absolutePath,
                        sizeKb = file.length() / 1024,
                        dateModified = file.lastModified(),
                        watchedCount = meta.watchedCount,
                        plannedCount = meta.plannedCount,
                        watchingCount = meta.watchingCount,
                        deviceName = meta.deviceName,
                        appVersion = meta.appVersion,
                        version = meta.version,
                        isAutoBackup = file.name.startsWith(AUTO_BACKUP_PREFIX)
                    )
                } catch (e: Exception) {
                    // Legacy backup without metadata
                    BackupFile(
                        name = file.name,
                        path = file.absolutePath,
                        sizeKb = file.length() / 1024,
                        dateModified = file.lastModified(),
                        isAutoBackup = file.name.startsWith(AUTO_BACKUP_PREFIX)
                    )
                }
            }
            ?.sortedByDescending { it.dateModified }
            ?: emptyList()
    }

    // Legacy: kept for gradual migration
    fun getBackupList(): List<BackupFile> = emptyList()

    suspend fun deleteBackup(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            require(file.delete()) { "Failed to delete file" }
            "Backup deleted"
        }
    }

    fun getBackupsDirPath(): String = backupDir.absolutePath
}

data class BackupFile(
    val name: String,
    val path: String,
    val sizeKb: Long,
    val dateModified: Long,
    val watchedCount: Int = -1,
    val plannedCount: Int = -1,
    val watchingCount: Int = -1,
    val deviceName: String = "",
    val appVersion: String = "",
    val version: Int = 1,
    val isAutoBackup: Boolean = false
)
