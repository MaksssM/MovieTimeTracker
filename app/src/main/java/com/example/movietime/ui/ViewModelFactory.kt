package com.example.movietime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.ui.details.DetailsViewModel
import com.example.movietime.ui.main.MainViewModel
import com.example.movietime.ui.search.SearchViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {

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
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}