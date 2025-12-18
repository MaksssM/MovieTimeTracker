package com.example.movietime.util

import android.content.Context
import android.util.Log
import com.example.movietime.data.db.WatchedItem

object Utils {
    private const val TAG = "Utils"

    fun formatMinutesToHoursAndMinutes(minutes: Int?): String {
        val mins = minutes ?: 0
        Log.d(TAG, "formatMinutesToHoursAndMinutes: input=$minutes, mins=$mins")

        if (mins <= 0) {
            return "0 год 0 хв"
        }
        val hours = mins / 60
        val remainingMinutes = mins % 60

        // Use short Ukrainian labels (abbreviations) for compactness
        val formatted = "${hours} год ${remainingMinutes} хв"
        Log.d(TAG, "formatMinutesToHoursAndMinutes: formatted='$formatted'")
        return formatted
    }

    /**
     * Compute total runtime for a TV show given episode runtime and number of watched episodes.
     * Defensive: treats null episodeRuntime as 0 and negative episodes as 0.
     */
    fun computeTotalRuntimeForTv(episodeRuntime: Int?, episodes: Int): Int {
        val ep = episodeRuntime ?: 0
        val eps = if (episodes < 0) 0 else episodes
        val result = ep * eps
        Log.d(TAG, "computeTotalRuntimeForTv: episodeRuntime=$ep, episodes=$eps, result=$result")
        return result
    }

    /**
     * Automatically determine TV show runtime and episodes with smart defaults
     */
    fun autoComputeTvShowRuntime(tvShow: com.example.movietime.data.model.TvShowResult): TvShowRuntimeInfo {
        Log.d(TAG, "autoComputeTvShowRuntime for: ${tvShow.name}")
        Log.d(TAG, "Status: ${tvShow.status}, InProduction: ${tvShow.inProduction}")
        Log.d(TAG, "Episodes: ${tvShow.numberOfEpisodes}, Seasons: ${tvShow.numberOfSeasons}")
        Log.d(TAG, "Episode runtime: ${tvShow.episodeRunTime}")

        val episodeRuntimeFromApi = tvShow.episodeRunTime?.firstOrNull()

        // Перевірка: якщо масив порожній або null, використовуємо оцінку
        val episodeRuntime = if (episodeRuntimeFromApi == null || episodeRuntimeFromApi == 0) {
            val estimated = estimateEpisodeRuntime(tvShow)
            Log.w(TAG, "⚠️ Episode runtime is null/empty for '${tvShow.name}', using estimated: $estimated min")
            estimated
        } else {
            episodeRuntimeFromApi
        }

        val totalEpisodes = tvShow.numberOfEpisodes
        val numberOfSeasons = tvShow.numberOfSeasons ?: 0
        val isOngoing = tvShow.status in listOf("Returning Series", "In Production") || tvShow.inProduction == true

        Log.d(TAG, "Processing: totalEpisodes=$totalEpisodes, numberOfSeasons=$numberOfSeasons, status=${tvShow.status}")

        // Визначаємо тип серіалу та розумні дефолти
        val smartDefaults = when {
            // Міні-серіал (1 сезон, менше 10 епізодів)
            numberOfSeasons == 1 && (totalEpisodes ?: 0) > 0 && (totalEpisodes ?: 0) <= 10 -> {
                Log.d(TAG, "Detected: Mini-series")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: 60, // Драматичні міні-серіали зазвичай 60 хв
                    episodes = totalEpisodes ?: 6, // Типовий міні-серіал 6 епізодів
                    description = "Міні-серіал"
                )
            }
            // Серіал що виходить (статус активний)
            isOngoing -> {
                Log.d(TAG, "Detected: Ongoing series")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: estimateEpisodeRuntime(tvShow),
                    episodes = estimateCurrentEpisodes(tvShow),
                    description = "Серіал що виходить"
                )
            }
            // Закінчений серіал
            tvShow.status == "Ended" -> {
                Log.d(TAG, "Detected: Ended series")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: estimateEpisodeRuntime(tvShow),
                    episodes = totalEpisodes ?: estimateCompletedEpisodes(numberOfSeasons),
                    description = "Закінчений серіал"
                )
            }
            // Скасований серіал
            tvShow.status == "Canceled" -> {
                Log.d(TAG, "Detected: Canceled series")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: estimateEpisodeRuntime(tvShow),
                    episodes = totalEpisodes ?: estimateCanceledEpisodes(numberOfSeasons),
                    description = "Скасований серіал"
                )
            }
            // Серіал з відомою кількістю епізодів
            totalEpisodes != null && totalEpisodes > 0 -> {
                Log.d(TAG, "Detected: Series with known episodes=$totalEpisodes")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: estimateEpisodeRuntime(tvShow),
                    episodes = totalEpisodes,
                    description = "Серіал"
                )
            }
            // Інші випадки
            else -> {
                Log.d(TAG, "Detected: Default series (estimating by seasons)")
                TvShowDefaults(
                    episodeRuntime = episodeRuntime ?: 45, // Стандартний час
                    episodes = if (numberOfSeasons > 0) numberOfSeasons * 12 else 12, // Приблизно 12 епізодів на сезон
                    description = "Серіал"
                )
            }
        }

        val totalMinutes = smartDefaults.episodeRuntime * smartDefaults.episodes

        // Валідація результатів
        if (smartDefaults.episodeRuntime <= 0 || smartDefaults.episodes <= 0) {
            Log.w(TAG, "⚠️ Invalid calculation: episodeRuntime=${smartDefaults.episodeRuntime}, episodes=${smartDefaults.episodes}")
            // Дефолтні значення, якщо розрахунок не вдався
            return TvShowRuntimeInfo(
                episodeRuntime = 45,
                episodes = 12,
                totalMinutes = 45 * 12,
                description = "Дефолт (помилка розрахунку)",
                isEstimated = true,
                isOngoing = isOngoing
            )
        }

        Log.d(TAG, "Smart defaults: ${smartDefaults.description}")
        Log.d(TAG, "Episode runtime: ${smartDefaults.episodeRuntime}, Episodes: ${smartDefaults.episodes}")
        Log.d(TAG, "Total minutes: $totalMinutes")

        return TvShowRuntimeInfo(
            episodeRuntime = smartDefaults.episodeRuntime,
            episodes = smartDefaults.episodes,
            totalMinutes = totalMinutes,
            description = smartDefaults.description,
            isEstimated = episodeRuntime == null || totalEpisodes == null,
            isOngoing = isOngoing
        )
    }

    private fun estimateEpisodeRuntime(tvShow: com.example.movietime.data.model.TvShowResult): Int {
        // Оцінка на основі жанрів, якщо доступно
        val genres = tvShow.genreIds ?: emptyList()
        return when {
            genres.contains(35) -> 22 // Comedy - 22 хв
            genres.contains(18) -> 50 // Drama - 50 хв
            genres.contains(9648) -> 45 // Mystery - 45 хв
            genres.contains(80) -> 45 // Crime - 45 хв
            genres.contains(10765) -> 45 // Sci-Fi - 45 хв
            else -> 45 // Стандартний час
        }
    }

    private fun estimateCurrentEpisodes(tvShow: com.example.movietime.data.model.TvShowResult): Int {
        val seasons = tvShow.numberOfSeasons ?: 1
        val totalEpisodes = tvShow.numberOfEpisodes

        return when {
            totalEpisodes != null -> totalEpisodes
            seasons <= 2 -> seasons * 10 // Нові серіали зазвичай 10 епізодів на сезон
            else -> seasons * 12 // Більш старі серіали 12+ епізодів
        }
    }

    private fun estimateCompletedEpisodes(seasons: Int): Int {
        return when {
            seasons <= 3 -> seasons * 10
            seasons <= 7 -> seasons * 15
            else -> seasons * 20 // Довгі серіали
        }
    }

    private fun estimateCanceledEpisodes(seasons: Int): Int {
        return seasons * 8 // Скасовані серіали зазвичай мають менше епізодів
    }

    data class TvShowDefaults(
        val episodeRuntime: Int,
        val episodes: Int,
        val description: String
    )

    data class TvShowRuntimeInfo(
        val episodeRuntime: Int,
        val episodes: Int,
        val totalMinutes: Int,
        val description: String,
        val isEstimated: Boolean,
        val isOngoing: Boolean
    )

    /**
     * Точно обчислює час серіалу на основі реальних даних про епізоди
     */
    fun computeExactTvShowRuntime(seasonsList: List<com.example.movietime.data.model.TvSeasonDetails>): TvShowExactRuntimeInfo {
        Log.d(TAG, "computeExactTvShowRuntime for ${seasonsList.size} seasons")

        val episodeRuntimes = mutableListOf<Int>()
        val episodeDetails = mutableListOf<EpisodeRuntimeInfo>()
        var totalMinutes = 0
        var totalEpisodes = 0

        for (season in seasonsList) {
            val seasonNumber = season.seasonNumber ?: 0
            val episodes = season.episodes ?: emptyList()

            Log.d(TAG, "Processing season $seasonNumber with ${episodes.size} episodes")

            for (episode in episodes) {
                val runtime = episode.runtime
                if (runtime != null && runtime > 0) {
                    episodeRuntimes.add(runtime)
                    totalMinutes += runtime
                    episodeDetails.add(
                        EpisodeRuntimeInfo(
                            seasonNumber = seasonNumber,
                            episodeNumber = episode.episodeNumber ?: 0,
                            runtime = runtime,
                            title = episode.name ?: "Episode ${episode.episodeNumber}"
                        )
                    )
                }
                totalEpisodes++
            }
        }

        // Статистика по тривалості епізодів
        val averageRuntime = if (episodeRuntimes.isNotEmpty()) {
            episodeRuntimes.average().toInt()
        } else 0

        val minRuntime = episodeRuntimes.minOrNull() ?: 0
        val maxRuntime = episodeRuntimes.maxOrNull() ?: 0
        val runtimeVariance = maxRuntime - minRuntime

        Log.d(TAG, "Exact runtime calculation:")
        Log.d(TAG, "Total episodes: $totalEpisodes, with runtime data: ${episodeRuntimes.size}")
        Log.d(TAG, "Total minutes: $totalMinutes, Average: $averageRuntime")
        Log.d(TAG, "Runtime range: $minRuntime - $maxRuntime min (variance: $runtimeVariance)")

        return TvShowExactRuntimeInfo(
            totalMinutes = totalMinutes,
            totalEpisodes = totalEpisodes,
            episodesWithRuntime = episodeRuntimes.size,
            averageEpisodeRuntime = averageRuntime,
            minEpisodeRuntime = minRuntime,
            maxEpisodeRuntime = maxRuntime,
            runtimeVariance = runtimeVariance,
            episodeDetails = episodeDetails,
            isComplete = episodeRuntimes.size == totalEpisodes
        )
    }

    data class TvShowExactRuntimeInfo(
        val totalMinutes: Int,
        val totalEpisodes: Int,
        val episodesWithRuntime: Int,
        val averageEpisodeRuntime: Int,
        val minEpisodeRuntime: Int,
        val maxEpisodeRuntime: Int,
        val runtimeVariance: Int,
        val episodeDetails: List<EpisodeRuntimeInfo>,
        val isComplete: Boolean
    ) {
        val completionPercentage: Int get() = if (totalEpisodes > 0) (episodesWithRuntime * 100) / totalEpisodes else 0
        val hasVariableRuntime: Boolean get() = runtimeVariance > 10 // Різниця більше 10 хвилин
    }

    data class EpisodeRuntimeInfo(
        val seasonNumber: Int,
        val episodeNumber: Int,
        val runtime: Int,
        val title: String
    )

    fun createWatchedItemFromMovie(
        id: Int,
        title: String?,
        name: String?,
        posterPath: String?,
        releaseDate: String?,
        runtime: Int?,
        mediaType: String,
        overview: String? = null,
        voteAverage: Double? = null,
        userRating: Float? = null
    ): WatchedItem {
        return WatchedItem(
            id = id,
            title = title ?: name ?: "Без назви",
            posterPath = posterPath,
            releaseDate = releaseDate,
            runtime = runtime ?: 0,
            mediaType = mediaType,
            overview = overview,
            voteAverage = voteAverage,
            userRating = userRating
        )
    }

    /**
     * Створює WatchedItem для серіалу з розумним визначенням параметрів
     */
    fun createWatchedItemFromTvShow(
        tvShow: com.example.movietime.data.model.TvShowResult,
        episodeRuntime: Int? = null,
        totalEpisodes: Int? = null,
        userRating: Float? = null
    ): WatchedItem {
        val runtimeInfo = if (episodeRuntime != null && totalEpisodes != null) {
            // Використовуємо дані користувача
            TvShowRuntimeInfo(
                episodeRuntime = episodeRuntime,
                episodes = totalEpisodes,
                totalMinutes = episodeRuntime * totalEpisodes,
                description = "Користувацькі дані",
                isEstimated = false,
                isOngoing = tvShow.status in listOf("Returning Series", "In Production")
            )
        } else {
            // Використовуємо автоматичне визначення
            autoComputeTvShowRuntime(tvShow)
        }

        return WatchedItem(
            id = tvShow.id,
            title = tvShow.name ?: "Без назви",
            posterPath = tvShow.posterPath,
            releaseDate = tvShow.firstAirDate,
            runtime = runtimeInfo.totalMinutes,
            mediaType = "tv",
            overview = tvShow.overview,
            voteAverage = tvShow.voteAverage.toDouble(),
            userRating = userRating,
            // TV-specific fields
            episodeRuntime = runtimeInfo.episodeRuntime,
            totalEpisodes = runtimeInfo.episodes,
            isOngoing = runtimeInfo.isOngoing,
            status = tvShow.status,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
