package com.example.movietime.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.db.PlannedDao
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.db.WatchingDao
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val watchedItemDao: WatchedItemDao,
    private val plannedDao: PlannedDao,
    private val watchingDao: WatchingDao,
    private val repository: AppRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    fun clearCache() {
        repository.clearAllCaches()
    }

    /**
     * Вызывается при смене языка ПЕРЕД перезапуском приложения.
     * Ставит флаг «нужен refresh», при следующем запуске init-блок
     * AppRepository обновит все тайтлы в БД.
     */
    fun onLanguageChanged() {
        languageManager.setLibraryRefreshNeeded()
        repository.clearAllCaches()
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Delete all watched items
                watchedItemDao.getAllSync().forEach { item ->
                    watchedItemDao.deleteById(item.id, item.mediaType)
                }
                // Delete all planned items
                plannedDao.deleteAll()
                // Delete all watching items
                watchingDao.deleteAll()
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error clearing data: ${e.message}")
            }
        }
    }
}