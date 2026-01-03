package com.example.movietime.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.backup.BackupManager
import com.example.movietime.data.backup.BackupFile
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

    private val _isCreatingBackup = MutableLiveData<Boolean>()
    val isCreatingBackup: LiveData<Boolean> = _isCreatingBackup

    private val _backupMessage = MutableLiveData<String>()
    val backupMessage: LiveData<String> = _backupMessage

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch {
            try {
                val backups = backupManager.getBackupList()
                _backupList.value = backups
            } catch (e: Exception) {
                _backupMessage.value = "Помилка при завантаженні резервних копій: ${e.message}"
            }
        }
    }

    suspend fun createBackup(): Result<String> {
        return backupManager.createBackup()
    }

    suspend fun createBackupToUri(uri: Uri): Result<String> {
        return backupManager.createBackupToUri(context, uri)
    }

    suspend fun restoreBackup(filePath: String): Result<String> {
        val result = backupManager.restoreBackup(filePath)
        if (result.isSuccess) {
            loadBackups()
        }
        return result
    }

    suspend fun deleteBackup(filePath: String): Result<String> {
        val result = backupManager.deleteBackup(filePath)
        if (result.isSuccess) {
            loadBackups()
        }
        return result
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<String> {
        return backupManager.restoreBackupFromUri(context, uri)
    }

    fun getBackupsDirPath(): String = backupManager.getBackupsDirPath()
}
