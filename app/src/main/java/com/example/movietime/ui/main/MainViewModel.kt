package com.example.movietime.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val watchedList: LiveData<List<WatchedItem>> = repository.getWatchedItems()

    val listIsEmpty: LiveData<Boolean> = watchedList.map { it.isEmpty() }
}