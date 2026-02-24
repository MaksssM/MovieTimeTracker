package com.example.movietime.service

import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationService @Inject constructor(
    private val repository: AppRepository,
    private val languageManager: LanguageManager
) {

    companion object {
        /** Минімальний рейтинг для фільтрації результатів */
        private const val MIN_VOTE_AVERAGE = 5.5
        /** Максимальна кількість "улюблених" елементів для запитів до API */
        private const val MAX_FAVORITES = 5
        /** Якщо Phase 1 дала менше цього числа — підключаємо жанр-дискавері */
        private const val FALLBACK_THRESHOLD = 8
        /** Максимум результатів у фінальному списку по кожному типу */
        private const val MAX_RESULTS_PER_TYPE = 20
    }

    // Track last refresh time to avoid unnecessary API calls
    private var lastRefreshTime = 0L
    private var cachedRecommendations: PersonalizedRecommendations? = null
    private val REFRESH_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes

    /**
     * Force clear cached recommendations so next call fetches fresh data
     */
    fun invalidateCache() {
        cachedRecommendations = null
        lastRefreshTime = 0L
    }

    /**
     * Генерує персоналізовані рекомендації на основі переглянутого контенту.
     *
     * Алгоритм:
     * 1. Оцінює кожен елемент: userRating (65%) + recency (30%) + watchCount-бонус (5%).
     * 2. Бере топ-5 за score — «улюблені».
     * 3. Phase 1: запитує TMDB /recommendations для кожного улюбленого.
     * 4. Phase 2 (fallback): якщо результатів мало — genre-discover з топ-жанрів.
     * 5. Фільтрує: voteAverage >= 5.5, без вже переглянутих.
     * 6. Сортує: вищий рейтинг + популярність вперед.
     */
    suspend fun getPersonalizedRecommendations(): PersonalizedRecommendations = withContext(Dispatchers.IO) {
        // Return cached if within refresh interval
        val now = System.currentTimeMillis()
        cachedRecommendations?.let { cached ->
            if (now - lastRefreshTime < REFRESH_INTERVAL_MS) {
                return@withContext cached
            }
        }

        val watchedItems = repository.getWatchedItemsSync()

        if (watchedItems.isEmpty()) {
            return@withContext PersonalizedRecommendations(emptyList(), emptyList())
        }

        // 1. Скоримо кожен переглянутий елемент
        val scoredItems = watchedItems.map { item ->
            val normalizedRating = ((item.userRating ?: item.voteAverage?.toFloat() ?: 5f)
                .coerceIn(0f, 10f)) / 10f

            // Recency: чим нещодавніше — тим вище. Параметр 0.015 → ~47 днів до 50% decay
            val recencyScore = item.lastUpdated?.let { ts ->
                val daysSince = (now - ts) / 86_400_000.0
                (1.0 / (1.0 + daysSince * 0.015)).coerceIn(0.0, 1.0)
            } ?: 0.4

            val watchBonus = if (item.watchCount > 1) 0.05 else 0.0
            val score = normalizedRating * 0.65 + recencyScore * 0.30 + watchBonus
            item to score
        }

        // 2. Топ MAX_FAVORITES за score → "улюблені"
        val favorites = scoredItems
            .sortedByDescending { it.second }
            .take(MAX_FAVORITES)
            .map { it.first }

        // Збираємо ідентифікатори вже переглянутого контенту (Watched / Planned / Watching / History)
        val seenMediaIds = repository.getAllSeenItemIds()
        val seenMovieIds = seenMediaIds.filter { it.mediaType == "movie" }.map { it.id }.toSet()
        val seenTvShowIds = seenMediaIds.filter { it.mediaType == "tv" }.map { it.id }.toSet()

        val recommendedMovies = mutableListOf<MovieResult>()
        val recommendedTvShows = mutableListOf<TvShowResult>()

        // 3. Phase 1: TMDB /recommendations для кожного улюбленого
        coroutineScope {
            favorites.map { item ->
                async {
                    try {
                        if (item.mediaType == "movie") {
                            repository.getMovieRecommendations(item.id)
                                .also { recs -> synchronized(recommendedMovies) { recommendedMovies.addAll(recs) } }
                        } else {
                            repository.getTvShowRecommendations(item.id)
                                .also { recs -> synchronized(recommendedTvShows) { recommendedTvShows.addAll(recs) } }
                        }
                    } catch (_: Exception) { /* ігноруємо окремі помилки */ }
                }
            }.forEach { it.await() }
        }

        // 4. Phase 2 (жанровий fallback) — якщо Phase 1 дала мало результатів
        val phase1MoviesOk = recommendedMovies
            .count { !seenMovieIds.contains(it.id) && it.voteAverage >= MIN_VOTE_AVERAGE.toFloat() }
        val phase1TvOk = recommendedTvShows
            .count { !seenTvShowIds.contains(it.id) && it.voteAverage >= MIN_VOTE_AVERAGE.toFloat() }

        if (phase1MoviesOk + phase1TvOk < FALLBACK_THRESHOLD) {
            // Топ-жанри з улюблених (оцінка >= 7)
            val likedItems = scoredItems
                .filter { (it.first.userRating ?: 0f) >= 7f }
                .map { it.first }
                .ifEmpty { favorites }

            fun extractTopGenres(mediaType: String): List<Int> =
                likedItems
                    .filter { it.mediaType == mediaType }
                    .flatMap { item ->
                        item.genreIds?.split(",")
                            ?.mapNotNull { g -> g.trim().toIntOrNull() }
                            ?: emptyList()
                    }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { it.key }

            val movieGenres = extractTopGenres("movie")
            val tvGenres = extractTopGenres("tv")

            coroutineScope {
                val discoverMoviesJob: kotlinx.coroutines.Deferred<List<com.example.movietime.data.model.MovieResult>>? =
                    if (movieGenres.isNotEmpty()) {
                        async {
                            try {
                                repository.discoverMovies(
                                    genreIds = movieGenres,
                                    minRating = MIN_VOTE_AVERAGE.toFloat(),
                                    sortBy = "vote_average.desc"
                                )
                            } catch (_: Exception) { emptyList<com.example.movietime.data.model.MovieResult>() }
                        }
                    } else null

                val discoverTvJob: kotlinx.coroutines.Deferred<List<com.example.movietime.data.model.TvShowResult>>? =
                    if (tvGenres.isNotEmpty()) {
                        async {
                            try {
                                repository.discoverTvShows(
                                    genreIds = tvGenres,
                                    minRating = MIN_VOTE_AVERAGE.toFloat(),
                                    sortBy = "vote_average.desc"
                                )
                            } catch (_: Exception) { emptyList<com.example.movietime.data.model.TvShowResult>() }
                        }
                    } else null

                discoverMoviesJob?.await()
                    ?.filter { !seenMovieIds.contains(it.id) }
                    ?.also { discovered -> synchronized(recommendedMovies) { recommendedMovies.addAll(discovered) } }

                discoverTvJob?.await()
                    ?.filter { !seenTvShowIds.contains(it.id) }
                    ?.also { discovered -> synchronized(recommendedTvShows) { recommendedTvShows.addAll(discovered) } }
            }
        }

        // 5. Фінальна дедублікація, фільтрація якості та сортування
        val finalMovies = recommendedMovies
            .filter { !seenMovieIds.contains(it.id) && it.voteAverage >= MIN_VOTE_AVERAGE.toFloat() }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<MovieResult> { it.voteAverage }
                    .thenByDescending { it.popularity }
            )
            .take(MAX_RESULTS_PER_TYPE)
            .shuffled() // Shuffle for variety on each load

        val finalTv = recommendedTvShows
            .filter { !seenTvShowIds.contains(it.id) && it.voteAverage >= MIN_VOTE_AVERAGE.toFloat() }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<TvShowResult> { it.voteAverage }
                    .thenByDescending { it.popularity }
            )
            .take(MAX_RESULTS_PER_TYPE)
            .shuffled() // Shuffle for variety on each load

        val result = PersonalizedRecommendations(finalMovies, finalTv)
        cachedRecommendations = result
        lastRefreshTime = System.currentTimeMillis()
        result
    }

    data class PersonalizedRecommendations(
        val movies: List<MovieResult>,
        val tvShows: List<TvShowResult>
    )
}
