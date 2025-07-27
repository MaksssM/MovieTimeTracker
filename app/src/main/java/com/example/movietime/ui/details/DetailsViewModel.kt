package com.example.movietime.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.launch

class DetailsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _mediaId = MutableLiveData<Pair<Int, String>>()

    val isWatched: LiveData<Boolean> = _mediaId.switchMap { (id, _) ->
        repository.isItemWatched(id).switchMap { item ->
            MutableLiveData(item != null)
        }
    }

    fun setMediaId(id: Int, mediaType: String) {
        _mediaId.value = Pair(id, mediaType)
    }

    fun toggleWatchedStatus(title: String, posterPath: String?) {
        viewModelScope.launch {
            val (id, mediaType) = _mediaId.value ?: return@launch
            val isCurrentlyWatched = isWatched.value ?: false

            if (isCurrentlyWatched) {
                repository.deleteWatchedItem(id)
            } else {
                val runtime = when (mediaType) {
                    "movie" -> repository.getMovieDetails(id)?.runtime
                    "tv" -> {
                        val details = repository.getTvShowDetails(id)
                        val singleEpisodeRuntime = details?.episodeRunTime?.firstOrNull() ?: 0
                        val totalEpisodes = details?.numberOfEpisodes ?: 0
                        if (singleEpisodeRuntime > 0 && totalEpisodes > 0) {
                            singleEpisodeRuntime * totalEpisodes
                        } else {
                            null
                        }
                    }
                    else -> null
                }

                if (runtime != null && runtime > 0) {
                    val item = WatchedItem(
                        id = id,
                        title = title,
                        posterPath = posterPath,
                        runtimeInMinutes = runtime,
                        mediaType = mediaType
                    )
                    repository.addWatchedItem(item)
                }
            }
        }
    }
}