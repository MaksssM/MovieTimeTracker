package com.example.movietime.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.model.Genre
import com.example.movietime.data.model.Person
import com.example.movietime.data.model.PersonRole
import com.example.movietime.data.model.SortOption
import com.example.movietime.data.model.CompanyResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

import com.example.movietime.util.LanguageManager

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AppRepository,
    private val api: TmdbApi,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val apiKey = BuildConfig.TMDB_API_KEY

    private val _searchResult = MutableLiveData<List<Any>>()
    val searchResult: LiveData<List<Any>> = _searchResult
    
    // For EnhancedSearchActivity - alias to searchResult to share logic
    val searchResults: LiveData<List<Any>> = _searchResult

    private val _popularContent = MutableLiveData<List<Any>>()
    val popularContent: LiveData<List<Any>> = _popularContent

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

    // ============ ADVANCED FILTERS ============
    
    // Genres
    private val _availableGenres = MutableLiveData<List<Genre>>(emptyList())
    val availableGenres: LiveData<List<Genre>> = _availableGenres
    
    private val _selectedGenres = MutableLiveData<List<Genre>>(emptyList())
    val selectedGenres: LiveData<List<Genre>> = _selectedGenres
    
    // People (actors/directors)
    private val _searchedPeople = MutableLiveData<List<Person>>(emptyList())
    val searchedPeople: LiveData<List<Person>> = _searchedPeople
    
    private val _selectedPerson = MutableLiveData<Person?>(null)
    val selectedPerson: LiveData<Person?> = _selectedPerson
    
    private val _selectedPersonRole = MutableLiveData<PersonRole>(PersonRole.ANY)
    val selectedPersonRole: LiveData<PersonRole> = _selectedPersonRole
    
    private val _popularPeople = MutableLiveData<List<Person>>(emptyList())
    val popularPeople: LiveData<List<Person>> = _popularPeople

    // Companies (studios)
    private val _searchedCompanies = MutableLiveData<List<CompanyResult>>(emptyList())
    val searchedCompanies: LiveData<List<CompanyResult>> = _searchedCompanies

    private val _selectedCompany = MutableLiveData<CompanyResult?>(null)
    val selectedCompany: LiveData<CompanyResult?> = _selectedCompany
    
    // Sort option
    private val _sortOption = MutableLiveData<SortOption>(SortOption.POPULARITY_DESC)
    val sortOption: LiveData<SortOption> = _sortOption
    
    // Year filter
    private val _selectedYear = MutableLiveData<Int?>(null)
    val selectedYear: LiveData<Int?> = _selectedYear

    
    // Duration filter (in minutes)
    private val _minDuration = MutableLiveData<Int?>(null)
    val minDuration: LiveData<Int?> = _minDuration
    
    private val _maxDuration = MutableLiveData<Int?>(null)
    val maxDuration: LiveData<Int?> = _maxDuration
    
    // Search mode
    private val _isAdvancedSearchMode = MutableLiveData<Boolean>(false)
    val isAdvancedSearchMode: LiveData<Boolean> = _isAdvancedSearchMode
    
    // Active filters count
    private val _activeFiltersCount = MutableLiveData<Int>(0)
    val activeFiltersCount: LiveData<Int> = _activeFiltersCount

    private var lastSearchQuery = ""
    private var lastSearchResults: List<Any> = emptyList()

    // Discovery Optimization
    private var seenItemIds = setOf<String>() // Set of "mediaType:id" strings
    
    init {
        refreshSeenItems()
        loadGenres()
    }

    private fun refreshSeenItems() {
        viewModelScope.launch {
            try {
                val ids = repository.getAllSeenItemIds()
                seenItemIds = ids.map { "${it.mediaType}:${it.id}" }.toSet()
                Log.d("SearchViewModel", "Refreshed seen items: ${seenItemIds.size}")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error refreshing seen items", e)
            }
        }
    }

    // Cache для результатів пошуку
    private val searchCache = mutableMapOf<String, List<Any>>()

    enum class FilterType {
        ALL, MOVIES, TV_SHOWS
    }

    // Pagination
    private var currentPage = 1
    private var isLastPage = false
    private var isSearching = false

    fun searchMulti(query: String, loadMore: Boolean = false) {
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        if (isSearching) return
        if (loadMore && isLastPage) return

        if (!loadMore) {
            currentPage = 1
            isLastPage = false
            lastSearchQuery = query
            addToSearchHistory(query)
            
             // Check cache ONLY for first page
            searchCache[query]?.let {
                lastSearchResults = it
                Log.d("SearchViewModel", "Using cached results for: $query")
                applyFiltersAndSort()
                return
            }
        } else {
            // Ensure we are continuing the same query
            if (query != lastSearchQuery) {
                // Should not happen if UI handles it right, but safety check
                currentPage = 1
                isLastPage = false
                lastSearchQuery = query
            }
        }

        // If API key not configured in BuildConfig, repository will throw — log a warning but continue.
        if (BuildConfig.TMDB_API_KEY.isBlank() || BuildConfig.TMDB_API_KEY.contains("YOUR_DEFAULT_KEY")) {
            Log.w("SearchViewModel", "TMDB API key looks unset.")
        }

        viewModelScope.launch {
            try {
                isSearching = true
                if (!loadMore) _isLoading.value = true // Show full loader only for first page
                
                val reqPage = if (loadMore) currentPage + 1 else 1
                var results = repository.searchMultiLanguage(query, reqPage)

                // Deduplicate against current results if loading more
                if (loadMore) {
                    val currentIds = lastSearchResults.mapNotNull { 
                        when(it) {
                            is MovieResult -> "movie_${it.id}"
                            is TvShowResult -> "tv_${it.id}"
                            else -> null
                        }
                    }.toSet()
                    
                    results = results.filter { item ->
                         val key = when (item) {
                            is MovieResult -> "movie_${item.id}"
                            is TvShowResult -> "tv_${item.id}"
                            else -> null
                        }
                        key != null && !currentIds.contains(key)
                    }
                } else {
                    results = removeDuplicates(results)
                }
                
                if (results.isEmpty()) {
                    isLastPage = true
                } else {
                    currentPage = reqPage
                    if (loadMore) {
                        lastSearchResults = lastSearchResults + results
                    } else {
                        lastSearchResults = results
                        searchCache[query] = results // Cache only page 1 for now to keep it simple
                    }
                    applyFiltersAndSort()
                }
                
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching content", e)
                if (!loadMore) {
                    _searchResult.value = emptyList()
                    _errorMessage.value = "Помилка при пошуці: ${e.localizedMessage ?: e.message}"
                }
            } finally {
                _isLoading.value = false
                isSearching = false
            }
        }
    }

    // Convenience methods for EnhancedSearchActivity tabs
    fun searchAll(query: String) {
        setFilterType(FilterType.ALL)
        searchMulti(query, false)
    }

    fun searchMovies(query: String) {
        setFilterType(FilterType.MOVIES)
        searchMulti(query, false)
    }

    fun searchTvShows(query: String) {
        setFilterType(FilterType.TV_SHOWS)
        searchMulti(query, false)
    }

    fun searchPeopleOnly(query: String, loadMore: Boolean = false) {
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        if (isSearching) return
        if (loadMore && isLastPage) return

        if (!loadMore) {
            currentPage = 1
            isLastPage = false
            lastSearchQuery = query
        }

        viewModelScope.launch {
            try {
                isSearching = true
                if (!loadMore) _isLoading.value = true

                val reqPage = if (loadMore) currentPage + 1 else 1
                val people = repository.searchPeople(query, "uk-UA", reqPage)
                
                if (people.isEmpty()) {
                    isLastPage = true
                } else {
                    currentPage = reqPage
                    // People deduplication
                    val newPeople = if (loadMore) {
                         val currentIds = lastSearchResults.filterIsInstance<Person>().map { it.id }.toSet()
                         people.filter { !currentIds.contains(it.id) }
                    } else {
                        people
                    }
                    
                    if (loadMore) {
                        lastSearchResults = lastSearchResults + newPeople
                    } else {
                        lastSearchResults = newPeople
                    }
                    
                    // For people search, we just show them directly usually, 
                    // but applyFiltersAndSort handles casting to list of Any
                    _searchResult.value = lastSearchResults
                    
                    Log.d("SearchViewModel", "People search returned ${newPeople.size} results for '$query' page $reqPage")
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching people", e)
                if (!loadMore) _searchResult.value = emptyList()
            } finally {
                _isLoading.value = false
                isSearching = false
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
        
        // Apply duration filter (for movies)
        val minDur = _minDuration.value
        val maxDur = _maxDuration.value
        if (minDur != null || maxDur != null) {
            filtered = filtered.filter { any ->
                when (any) {
                    is MovieResult -> {
                        val runtime = any.runtime ?: 0
                        val passesMin = minDur == null || runtime >= minDur
                        val passesMax = maxDur == null || runtime <= maxDur
                        passesMin && passesMax
                    }
                    is TvShowResult -> {
                        // For TV shows, use episode runtime if available
                        val episodeRuntime = any.episodeRunTime?.firstOrNull() ?: 0
                        val passesMin = minDur == null || episodeRuntime >= minDur
                        val passesMax = maxDur == null || episodeRuntime <= maxDur
                        passesMin && passesMax
                    }
                    else -> true
                }
            }.toMutableList()
        }

        
        // Apply person filter (actor, director, writer, producer)
        
        // Discovery Optimization: Penalize items user has already seen/clicked/planned
        // Use a composite score for sorting: (original_rank_value * penalty_factor)
        
        val sortOption = _sortOption.value ?: SortOption.POPULARITY_DESC
        
        filtered.sortWith(Comparator { a, b ->
            val scoreA = calculateDiscoveryScore(a, sortOption)
            val scoreB = calculateDiscoveryScore(b, sortOption)
            scoreB.compareTo(scoreA) // Descending
        })

        // Apply results limit - removed for infinite scroll support
        // val limit = _resultsLimit.value ?: 50
        // if (filtered.size > limit) {
        //     filtered = filtered.take(limit).toMutableList()
        // }

        _searchResult.value = filtered
    }

    private fun calculateDiscoveryScore(item: Any, option: SortOption): Double {
        val (id, mediaType, rawScore) = when (item) {
            is MovieResult -> Triple(item.id, "movie", when(option) {
                SortOption.VOTE_AVERAGE_DESC, SortOption.VOTE_AVERAGE_ASC -> item.voteAverage?.toDouble() ?: 0.0
                SortOption.RELEASE_DATE_DESC, SortOption.RELEASE_DATE_ASC -> (item.releaseDate?.substringBefore("-")?.toDoubleOrNull() ?: 0.0) / 2025.0
                else -> item.popularity?.toDouble() ?: 0.0
            })
            is TvShowResult -> Triple(item.id, "tv", when(option) {
                SortOption.VOTE_AVERAGE_DESC, SortOption.VOTE_AVERAGE_ASC -> item.voteAverage?.toDouble() ?: 0.0
                SortOption.RELEASE_DATE_DESC, SortOption.RELEASE_DATE_ASC -> (item.firstAirDate?.substringBefore("-")?.toDoubleOrNull() ?: 0.0) / 2025.0
                else -> item.popularity?.toDouble() ?: 0.0
            })
            else -> Triple(0, "unknown", 0.0)
        }

        val isSeen = seenItemIds.contains("$mediaType:$id")
        val penaltyFactor = if (isSeen) 0.3 else 1.0 // 70% penalty for seen items to ensure discovery
        
        return rawScore * penaltyFactor
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
        _selectedYear.value = null
        _minDuration.value = null
        _maxDuration.value = null
        applyFiltersAndSort()
    }
    
    fun setDurationFilter(minMinutes: Int?, maxMinutes: Int?) {
        _minDuration.value = minMinutes
        _maxDuration.value = maxMinutes
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
        _resultsLimit.value = limit
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
        val seen = mutableSetOf<String>()
        return items.filter { item ->
            val key = when (item) {
                is MovieResult -> "movie_${item.id}"
                is TvShowResult -> "tv_${item.id}"
                else -> return@filter true
            }
            seen.add(key)
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


    fun loadPopularContent() {
        viewModelScope.launch {
            try {
                // Generate random page (1-20) for more variety
                val randomPage = (1..20).random()
                
                // Use discover API instead of popular for random content
                val discoverMoviesResponse = api.discoverMovies(
                    apiKey = apiKey,
                    language = languageManager.getApiLanguage(),
                    page = randomPage,
                    sortBy = "vote_count.desc"
                )
                val discoverTvShowsResponse = api.discoverTvShows(
                    apiKey = apiKey,
                    language = languageManager.getApiLanguage(),
                    page = randomPage,
                    sortBy = "vote_count.desc"
                )

                val randomMovies = discoverMoviesResponse.results.shuffled()
                val randomTvShows = discoverTvShowsResponse.results.shuffled()

                val combined = mutableListOf<Any>()
                combined.addAll(randomMovies)
                combined.addAll(randomTvShows)
                
                // Sort combined list by discovery score to prioritize new items
                val sortedCombined = combined.sortedByDescending { item ->
                    calculateDiscoveryScore(item, SortOption.POPULARITY_DESC)
                }
                
                // Shuffle the final selection a bit for extra variety
                _popularContent.value = sortedCombined.shuffled()
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading random content", e)
                _popularContent.value = emptyList()
            }
        }
    }

    // --- Search History Methods ---
    
    fun addMovieToSearchHistory(movie: MovieResult) {
        viewModelScope.launch {
            try {
                repository.addMovieToSearchHistory(movie)
                Log.d("SearchViewModel", "Added movie to search history: ${movie.title}")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error adding movie to search history", e)
            }
        }
    }
    
    fun addTvShowToSearchHistory(tvShow: TvShowResult) {
        viewModelScope.launch {
            try {
                repository.addTvShowToSearchHistory(tvShow)
                Log.d("SearchViewModel", "Added TV show to search history: ${tvShow.name}")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error adding TV show to search history", e)
            }
        }
    }


    // ============ ADVANCED SEARCH METHODS ============
    
    fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = repository.getAllGenres()
                _availableGenres.value = genres
                Log.d("SearchViewModel", "Loaded ${genres.size} genres")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading genres", e)
            }
        }
    }
    
    fun toggleGenre(genre: Genre) {
        val current = _selectedGenres.value?.toMutableList() ?: mutableListOf()
        if (current.any { it.id == genre.id }) {
            current.removeAll { it.id == genre.id }
        } else {
            current.add(genre)
        }
        _selectedGenres.value = current
        updateActiveFiltersCount()
    }
    
    fun clearSelectedGenres() {
        _selectedGenres.value = emptyList()
        updateActiveFiltersCount()
    }
    
    fun isGenreSelected(genre: Genre): Boolean {
        return _selectedGenres.value?.any { it.id == genre.id } ?: false
    }
    
    fun searchPeople(query: String) {
        if (query.length < 2) {
            _searchedPeople.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val people = repository.searchPeople(query)
                _searchedPeople.value = people
                Log.d("SearchViewModel", "Found ${people.size} people for '$query'")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching people", e)
                _searchedPeople.value = emptyList()
            }
        }
    }
    
    fun loadPopularPeople() {
        viewModelScope.launch {
            try {
                val people = repository.getPopularPeople()
                _popularPeople.value = people
                Log.d("SearchViewModel", "Loaded ${people.size} popular people")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading popular people", e)
            }
        }
    }

    fun searchCompanies(query: String) {
        if (query.length < 2) {
            _searchedCompanies.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val companies = repository.searchCompanies(query)
                _searchedCompanies.value = companies
                Log.d("SearchViewModel", "Found ${companies.size} companies for '$query'")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching companies", e)
                _searchedCompanies.value = emptyList()
            }
        }
    }

    fun selectCompany(company: CompanyResult?) {
        _selectedCompany.value = company
        updateActiveFiltersCount()
    }
    
    fun selectPerson(person: Person?) {
        _selectedPerson.value = person
        updateActiveFiltersCount()
    }
    
    fun setPersonRole(role: PersonRole) {
        _selectedPersonRole.value = role
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    fun setSelectedYear(year: Int?) {
        _selectedYear.value = year
        updateActiveFiltersCount()
    }
    
    fun setAdvancedSearchMode(enabled: Boolean) {
        _isAdvancedSearchMode.value = enabled
    }
    
    private fun updateActiveFiltersCount() {
        var count = 0
        if (!_selectedGenres.value.isNullOrEmpty()) count++
        if (_selectedPerson.value != null) count++
        if (_selectedCompany.value != null) count++
        if (_selectedYear.value != null) count++
        if ((_minRating.value ?: 0.0) > 0) count++
        _activeFiltersCount.value = count
    }
    
    fun discoverWithFilters() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val mediaType = when (_filterType.value) {
                    FilterType.MOVIES -> "movie"
                    FilterType.TV_SHOWS -> "tv"
                    else -> "all"
                }
                
                val genreIds = _selectedGenres.value?.map { it.id }?.takeIf { it.isNotEmpty() }
                val personId = _selectedPerson.value?.id
                val personRole = _selectedPersonRole.value ?: PersonRole.ANY
                val minRating = _minRating.value?.toFloat()?.takeIf { it > 0 }
                val year = _selectedYear.value
                val companyId = _selectedCompany.value?.id
                val sortBy = _sortOption.value?.apiValue ?: "popularity.desc"
                
                Log.d("SearchViewModel", "Discover with filters: type=$mediaType, genres=$genreIds, person=$personId, role=$personRole, company=$companyId, minRating=$minRating, year=$year, sortBy=$sortBy")
                
                val results = repository.discoverByFilters(
                    mediaType = mediaType,
                    genreIds = genreIds,
                    personId = personId,
                    personRole = personRole,
                    minRating = minRating,
                    companyId = companyId,
                    year = year,
                    sortBy = sortBy
                )
                
                lastSearchResults = results
                _searchResult.value = results
                _errorMessage.value = null
                
                Log.d("SearchViewModel", "Discover returned ${results.size} results")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering content", e)
                _searchResult.value = emptyList()
                _errorMessage.value = "Помилка при пошуці: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetAdvancedFilters() {
        _selectedGenres.value = emptyList()
        _selectedPerson.value = null
        _selectedCompany.value = null
        _selectedPersonRole.value = PersonRole.ANY
        _selectedYear.value = null
        _sortOption.value = SortOption.POPULARITY_DESC
        _minRating.value = 0.0
        updateActiveFiltersCount()
    }
    
    fun hasActiveFilters(): Boolean {
        return !_selectedGenres.value.isNullOrEmpty() ||
                _selectedPerson.value != null ||
                _selectedCompany.value != null ||
                _selectedYear.value != null ||
                (_minRating.value ?: 0.0) > 0
    }
    
    fun getActiveFiltersDescription(): String {
        val parts = mutableListOf<String>()
        
        _selectedGenres.value?.takeIf { it.isNotEmpty() }?.let { genres ->
            parts.add("Жанри: ${genres.joinToString(", ") { it.name }}")
        }
        
        _selectedPerson.value?.let { person ->
            val roleText = when (_selectedPersonRole.value) {
                PersonRole.ACTOR -> "(актор)"
                PersonRole.DIRECTOR -> "(режисер)"
                PersonRole.WRITER -> "(сценарист)"
                PersonRole.PRODUCER -> "(продюсер)"
                else -> ""
            }
            parts.add("${person.name} $roleText")
        }

        _selectedCompany.value?.let { company ->
            parts.add("Студія: ${company.name ?: "—"}")
        }
        
        _selectedYear.value?.let { year ->
            parts.add("Рік: $year")
        }
        
        _minRating.value?.takeIf { it > 0 }?.let { rating ->
            parts.add("Рейтинг ≥ ${"%.1f".format(rating)}")
        }
        
        return if (parts.isEmpty()) "Фільтри не вибрані" else parts.joinToString(" • ")
    }
}
