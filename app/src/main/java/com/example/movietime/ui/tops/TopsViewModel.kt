package com.example.movietime.ui.tops

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.R
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val rating: Float,
    val mediaType: String
)

data class TopRow(
    val titleRes: Int,
    val items: List<TopItem>
)

private data class RowSpec(
    val titleRes: Int,
    val mediaType: String,
    val genreId: Int?,
    val minVotes: Int
)

@HiltViewModel
class TopsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _rows = MutableLiveData<List<TopRow>>()
    val rows: LiveData<List<TopRow>> = _rows

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<Boolean>()
    val error: LiveData<Boolean> = _error

    private val specs = listOf(
        RowSpec(R.string.top_rated_movies, "movie", null, 500),
        RowSpec(R.string.top_rated_tv_shows, "tv", null, 200),
        RowSpec(R.string.category_action, "movie", 28, 100),
        RowSpec(R.string.category_comedy, "movie", 35, 100),
        RowSpec(R.string.category_drama, "movie", 18, 100),
        RowSpec(R.string.category_scifi, "movie", 878, 100),
        RowSpec(R.string.category_horror, "movie", 27, 50),
        RowSpec(R.string.category_animation, "movie", 16, 100)
    )

    fun loadTops() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = false
            try {
                val deferred = specs.map { spec ->
                    async {
                        val items = if (spec.mediaType == "movie") {
                            repository.discoverTopMovies(spec.genreId, spec.minVotes)
                                .map {
                                    TopItem(
                                        id = it.id,
                                        title = it.title ?: "",
                                        posterPath = it.posterPath,
                                        rating = it.voteAverage,
                                        mediaType = "movie"
                                    )
                                }
                        } else {
                            repository.discoverTopTvShows(spec.genreId, spec.minVotes)
                                .map {
                                    TopItem(
                                        id = it.id,
                                        title = it.name ?: "",
                                        posterPath = it.posterPath,
                                        rating = it.voteAverage,
                                        mediaType = "tv"
                                    )
                                }
                        }
                        TopRow(spec.titleRes, items)
                    }
                }
                val rows = deferred.awaitAll().filter { it.items.isNotEmpty() }
                _rows.value = rows
                _error.value = rows.isEmpty()
            } catch (e: Exception) {
                _rows.value = emptyList()
                _error.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
