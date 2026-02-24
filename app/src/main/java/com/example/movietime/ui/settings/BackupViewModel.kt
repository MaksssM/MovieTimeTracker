package com.example.movietime.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.backup.BackupFile
import com.example.movietime.data.backup.BackupManager
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    private val backupManager = BackupManager(context, repository)

    private val _backupList = MutableLiveData<List<BackupFile>>()
    val backupList: LiveData<List<BackupFile>> = _backupList

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _backupMessage = MutableLiveData<String>()
    val backupMessage: LiveData<String> = _backupMessage

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch {
            try {
                val backups = backupManager.getBackupListAsync()
                _backupList.value = backups
            } catch (e: Exception) {
                _backupMessage.value = "Ошибка при загрузке резервных копий: ${e.message}"
            }
        }
    }

    /** Direct suspend call — use from coroutines in UI to avoid observer pattern. */
    suspend fun getBackupListDirect(): List<BackupFile> =
        backupManager.getBackupListAsync()

    suspend fun createBackup(): Result<String> =
        backupManager.createBackup()

    suspend fun createBackupToUri(uri: Uri): Result<String> =
        backupManager.createBackupToUri(context, uri)

    /** Creates an automatic safety backup (e.g., before restore). */
    suspend fun createAutoBackup(): Result<String> =
        backupManager.createAutoBackup()

    /**
     * Restores from a local backup file.
     * @param merge If true — only adds items missing from the collection.
     *              If false — clears all data first (full replacement).
     */
    suspend fun restoreBackup(filePath: String, merge: Boolean = false): Result<String> {
        _isLoading.postValue(true)
        val result = backupManager.restoreBackup(filePath, merge)
        _isLoading.postValue(false)
        if (result.isSuccess) loadBackups()
        return result
    }

    suspend fun deleteBackup(filePath: String): Result<String> {
        val result = backupManager.deleteBackup(filePath)
        if (result.isSuccess) loadBackups()
        return result
    }

    /**
     * Restores from an external URI (file picker).
     * @param merge If true — merge mode (add missing); if false — full replacement.
     */
    suspend fun restoreBackupFromUri(uri: Uri, merge: Boolean = false): Result<String> {
        _isLoading.postValue(true)
        val result = backupManager.restoreBackupFromUri(context, uri, merge)
        _isLoading.postValue(false)
        return result
    }

    fun getBackupsDirPath(): String = backupManager.getBackupsDirPath()
}

