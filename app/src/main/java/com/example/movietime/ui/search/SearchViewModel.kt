package com.example.movietime.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _searchResult = MutableLiveData<List<Any>>()
    val searchResult: LiveData<List<Any>> = _searchResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Filter options
    private val _filterType = MutableLiveData<FilterType>(FilterType.ALL)
    val filterType: LiveData<FilterType> = _filterType

    private val _sortByRating = MutableLiveData<Boolean>(false)
    @Suppress("unused")
    val sortByRating: LiveData<Boolean> = _sortByRating

    private val _sortByPopularity = MutableLiveData<Boolean>(false)
    @Suppress("unused")
    val sortByPopularity: LiveData<Boolean> = _sortByPopularity

    private val _minRating = MutableLiveData<Double>(0.0)
    @Suppress("unused")
    val minRating: LiveData<Double> = _minRating

    private val _searchHistory = MutableLiveData<List<String>>(emptyList())
    @Suppress("unused")
    val searchHistory: LiveData<List<String>> = _searchHistory

    // Улюблені пошукові запити
    private val _favoriteSearches = MutableLiveData<List<String>>(emptyList())
    @Suppress("unused")
    val favoriteSearches: LiveData<List<String>> = _favoriteSearches

    // Обмеження кількості результатів
    private val _resultsLimit = MutableLiveData<Int>(50)
    @Suppress("unused")
    val resultsLimit: LiveData<Int> = _resultsLimit

    private var lastSearchQuery = ""
    private var lastSearchResults: List<Any> = emptyList()

    // Cache для результатів пошуку
    private val searchCache = mutableMapOf<String, List<Any>>()

    enum class FilterType {
        ALL, MOVIES, TV_SHOWS
    }

    fun searchMulti(query: String) {
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        lastSearchQuery = query
        addToSearchHistory(query)

        // Перевірити кеш
        searchCache[query]?.let {
            lastSearchResults = it
            Log.d("SearchViewModel", "Using cached results for: $query (cache size: ${searchCache.size})")
            applyFiltersAndSort()
            return
        }

        // If API key not configured in BuildConfig, repository will throw — log a warning but continue.
        if (BuildConfig.TMDB_API_KEY.isBlank() || BuildConfig.TMDB_API_KEY.contains("YOUR_DEFAULT_KEY") || BuildConfig.TMDB_API_KEY.contains("YOUR")) {
            Log.w("SearchViewModel", "TMDB API key looks unset in BuildConfig; ensure AppModule provides correct key.")
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                var results = repository.searchMultiLanguage(query)

                // Видалити дублювання
                results = removeDuplicates(results)

                lastSearchResults = results
                searchCache[query] = results  // Зберегти в кеш
                applyFiltersAndSort()
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching content", e)
                _searchResult.value = emptyList()
                _errorMessage.value = "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043f\u043e\u0438\u0441\u043a\u0435: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilterType(type: FilterType) {
        _filterType.value = type
        applyFiltersAndSort()
    }

    fun setSortByRating(sort: Boolean) {
        _sortByRating.value = sort
        applyFiltersAndSort()
    }

    fun setSortByPopularity(sort: Boolean) {
        _sortByPopularity.value = sort
        applyFiltersAndSort()
    }

    @Suppress("unused")
    fun setMinRating(value: Double) {
        _minRating.value = value
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var filtered = lastSearchResults.toMutableList()

        // Apply type filter
        when (_filterType.value) {
            FilterType.MOVIES -> {
                filtered = filtered.filterIsInstance<MovieResult>().toMutableList()
            }
            FilterType.TV_SHOWS -> {
                filtered = filtered.filterIsInstance<TvShowResult>().toMutableList()
            }
            else -> {} // ALL - no filter
        }

        // Apply rating filter
        val minR = _minRating.value ?: 0.0
        if (minR > 0.0) {
            filtered = filtered.filter { any ->
                when (any) {
                    is MovieResult -> (any.voteAverage?.toDouble() ?: 0.0) >= minR
                    is TvShowResult -> (any.voteAverage?.toDouble() ?: 0.0) >= minR
                    else -> false
                }
            }.toMutableList()
        }

        // Apply sorting by rating
        if (_sortByRating.value == true) {
            filtered.sortByDescending { item ->
                when (item) {
                    is MovieResult -> item.voteAverage?.toDouble() ?: 0.0
                    is TvShowResult -> item.voteAverage?.toDouble() ?: 0.0
                    else -> 0.0
                }
            }
        }

        // Apply sorting by popularity (if rating sort is not active)
        if (_sortByPopularity.value == true && _sortByRating.value != true) {
            filtered.sortByDescending { item ->
                when (item) {
                    is MovieResult -> item.popularity?.toDouble() ?: 0.0
                    is TvShowResult -> item.popularity?.toDouble() ?: 0.0
                    else -> 0.0
                }
            }
        }

        // Apply results limit
        val limit = _resultsLimit.value ?: 50
        if (filtered.size > limit) {
            filtered = filtered.take(limit).toMutableList()
        }

        _searchResult.value = filtered
    }

    private fun addToSearchHistory(query: String) {
        if (query.isBlank()) return

        val history = _searchHistory.value?.toMutableList() ?: mutableListOf()

        // Remove if already exists (avoid duplicates)
        history.removeAll { it.equals(query, ignoreCase = true) }

        // Add to beginning
        history.add(0, query)

        // Keep only last 10 searches
        if (history.size > 10) {
            history.removeAt(history.size - 1)
        }

        _searchHistory.value = history
    }

    @Suppress("unused")
    fun removeHistoryItem(query: String) {
        val history = _searchHistory.value?.toMutableList() ?: return
        if (history.remove(query)) {
            _searchHistory.value = history
        }
    }

    @Suppress("unused")
    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    @Suppress("unused")
    fun resetFilters() {
        _filterType.value = FilterType.ALL
        _sortByRating.value = false
        _sortByPopularity.value = false
        _minRating.value = 0.0
        applyFiltersAndSort()
    }

    @Suppress("unused")
    fun getSearchResultsCount(): Int {
        return _searchResult.value?.size ?: 0
    }

    @Suppress("unused")
    fun getMoviesCount(): Int {
        return lastSearchResults.filterIsInstance<MovieResult>().size
    }

    @Suppress("unused")
    fun getTvShowsCount(): Int {
        return lastSearchResults.filterIsInstance<TvShowResult>().size
    }

    @Suppress("unused")
    fun getGroupedResults(): List<GroupedSearchItem> {
        val grouped = mutableListOf<GroupedSearchItem>()

        val movies = _searchResult.value?.filterIsInstance<MovieResult>() ?: emptyList()
        val tvShows = _searchResult.value?.filterIsInstance<TvShowResult>() ?: emptyList()

        if (movies.isNotEmpty()) {
            grouped.add(GroupedSearchItem(GroupedSearchItem.ItemType.HEADER, "🎬 ФІЛЬМИ (${movies.size})"))
            movies.forEach { grouped.add(GroupedSearchItem(GroupedSearchItem.ItemType.MOVIE, it)) }
        }

        if (tvShows.isNotEmpty()) {
            grouped.add(GroupedSearchItem(GroupedSearchItem.ItemType.HEADER, "📺 СЕРІАЛИ (${tvShows.size})"))
            tvShows.forEach { grouped.add(GroupedSearchItem(GroupedSearchItem.ItemType.TV_SHOW, it)) }
        }

        return grouped
    }

    @Suppress("unused")
    fun toggleFavoriteSearch(query: String) {
        val favorites = _favoriteSearches.value?.toMutableList() ?: mutableListOf()

        if (favorites.contains(query)) {
            favorites.remove(query)
            Log.d("SearchViewModel", "Removed from favorites: $query")
        } else {
            favorites.add(0, query)
            Log.d("SearchViewModel", "Added to favorites: $query")
        }

        _favoriteSearches.value = favorites
    }

    @Suppress("unused")
    fun isFavorite(query: String): Boolean {
        return _favoriteSearches.value?.contains(query) ?: false
    }

    @Suppress("unused")
    fun setResultsLimit(limit: Int) {
        _resultsLimit.value = limit.coerceIn(10, 100)
        applyFiltersAndSort()
    }

    @Suppress("unused")
    fun saveFilterPreferences(context: android.content.Context) {
        val prefs = context.getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("filter_type", _filterType.value?.name ?: FilterType.ALL.name)
            putBoolean("sort_by_rating", _sortByRating.value ?: false)
            putBoolean("sort_by_popularity", _sortByPopularity.value ?: false)
            putFloat("min_rating", (_minRating.value ?: 0.0).toFloat())
            putInt("results_limit", _resultsLimit.value ?: 50)
            apply()
        }
    }

    @Suppress("unused")
    fun loadFilterPreferences(context: android.content.Context) {
        val prefs = context.getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE)

        val filterTypeName = prefs.getString("filter_type", FilterType.ALL.name) ?: FilterType.ALL.name
        _filterType.value = try {
            FilterType.valueOf(filterTypeName)
        } catch (e: Exception) {
            FilterType.ALL
        }

        _sortByRating.value = prefs.getBoolean("sort_by_rating", false)
        _sortByPopularity.value = prefs.getBoolean("sort_by_popularity", false)
        _minRating.value = prefs.getFloat("min_rating", 0f).toDouble()
        _resultsLimit.value = prefs.getInt("results_limit", 50)
    }

    // Приватна функція для усереднення популярності
    private fun getAveragePopularity(items: List<Any>): Double {
        val popularities = items.mapNotNull { item ->
            when (item) {
                is MovieResult -> item.popularity
                is TvShowResult -> item.popularity
                else -> null
            }
        }
        return if (popularities.isNotEmpty()) popularities.average() else 0.0
    }

    // Видалення дублювань результатів
    private fun removeDuplicates(items: List<Any>): List<Any> {
        val seen = mutableSetOf<Int>()
        return items.filter { item ->
            val id = when (item) {
                is MovieResult -> item.id
                is TvShowResult -> item.id
                else -> return@filter true
            }
            seen.add(id)
        }
    }

    @Suppress("unused")
    fun exportFavoriteSearches(): String {
        return (_favoriteSearches.value ?: emptyList()).joinToString(",")
    }

    @Suppress("unused")
    fun importFavoriteSearches(data: String) {
        if (data.isBlank()) return

        val searches = data.split(",").filter { it.isNotBlank() }
        _favoriteSearches.value = searches
    }

    @Suppress("unused")
    fun clearSearchCache() {
        searchCache.clear()
    }

    @Suppress("unused")
    fun getCacheSize(): Int {
        return searchCache.size
    }
}
