package com.example.movietime.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.AppDatabase
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.ui.details.DetailsViewModel
import com.example.movietime.ui.main.MainViewModel
import com.example.movietime.ui.search.SearchViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    // Создаем репозиторий один раз, чтобы он был доступен для всех ViewModel
    private val repository: AppRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        val api = TmdbApi.create()
        AppRepository(db.watchedItemDao(), api)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DetailsViewModel::class.java) -> {
                DetailsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}