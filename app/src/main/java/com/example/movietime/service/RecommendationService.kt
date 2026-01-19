package com.example.movietime.service

import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationService @Inject constructor(
    private val repository: AppRepository
) {

    /**
     * Генерує персональні рекомендації на основі переглянутого контенту.
     * Алгоритм:
     * 1. Знаходить контент з найвищим рейтингом (userRating >= 8 або просто останні, якщо немає оцінок).
     * 2. Вибирає топ-5 улюблених елементів.
     * 3. Запитує API рекомендації для кожного з них.
     * 4. Об'єднує результати, видаляє дублікати та те, що вже переглянуто.
     */
    suspend fun getPersonalizedRecommendations(): PersonalizedRecommendations = withContext(Dispatchers.IO) {
        val watchedItems = repository.getWatchedItemsSync()
        
        if (watchedItems.isEmpty()) {
            return@withContext PersonalizedRecommendations(emptyList(), emptyList())
        }

        // 1. Фільтруємо найкращі (оцінка >= 8 або просто останні 10)
        val favorites = watchedItems.filter { (it.userRating ?: 0f) >= 8f }
            .ifEmpty { watchedItems.takeLast(10) }
            .shuffled() // Додаємо випадковості

        val recommendedMovies = mutableListOf<MovieResult>()
        val recommendedTvShows = mutableListOf<TvShowResult>()

        coroutineScope {
            favorites.forEach { item ->
                launch {
                    try {
                        if (item.mediaType == "movie") {
                            val recs = repository.getMovieRecommendations(item.id)
                            synchronized(recommendedMovies) {
                                recommendedMovies.addAll(recs)
                            }
                        } else {
                            val recs = repository.getTvShowRecommendations(item.id)
                            synchronized(recommendedTvShows) {
                                recommendedTvShows.addAll(recs)
                            }
                        }
                    } catch (_: Exception) {
                        // Ігноруємо помилки окремих запитів
                    }
                }
            }
        }

        // Видаляємо дублікати і те, що вже бачили (Watched, Planned, Watching, Search History)
        val seenMediaIds = repository.getAllSeenItemIds()
        val seenMovieIds = seenMediaIds.filter { it.mediaType == "movie" }.map { it.id }.toSet()
        val seenTvShowIds = seenMediaIds.filter { it.mediaType == "tv" }.map { it.id }.toSet()
        
        val uniqueMovies = recommendedMovies
            .filter { !seenMovieIds.contains(it.id) }
            .distinctBy { it.id }
            .sortedByDescending { it.voteAverage }

        val uniqueTvShows = recommendedTvShows
            .filter { !seenTvShowIds.contains(it.id) }
            .distinctBy { it.id }
            .sortedByDescending { it.voteAverage }

        PersonalizedRecommendations(uniqueMovies, uniqueTvShows)
    }

    data class PersonalizedRecommendations(
        val movies: List<MovieResult>,
        val tvShows: List<TvShowResult>
    )
}
