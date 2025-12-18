package com.example.movietime.service

import android.util.Log
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервіс для отримання точних даних про серіали з деталями кожного епізоду
 */
@Singleton
class TvShowEpisodeService @Inject constructor(
    private val repository: AppRepository
) {
    companion object {
        private const val TAG = "TvShowEpisodeService"
    }

    /**
     * Отримує точну інформацію про серіал включаючи тривалість кожного епізоду
     */
    suspend fun getExactTvShowRuntime(tvId: Int, totalSeasons: Int): ExactRuntimeResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting exact runtime for TV show $tvId with $totalSeasons seasons")

        try {
            // Отримуємо детальну інформацію про всі сезони
            val seasonsDetails = repository.getAllSeasonsDetails(tvId, totalSeasons)

            if (seasonsDetails.isEmpty()) {
                Log.w(TAG, "No season details loaded for TV show $tvId")
                return@withContext ExactRuntimeResult.Error("Не вдалося завантажити дані про сезони")
            }

            // Обчислюємо точний час на основі реальних даних епізодів
            val exactInfo = Utils.computeExactTvShowRuntime(seasonsDetails)

            Log.d(TAG, "Exact runtime calculation completed:")
            Log.d(TAG, "Total: ${exactInfo.totalMinutes} min from ${exactInfo.episodesWithRuntime}/${exactInfo.totalEpisodes} episodes")
            Log.d(TAG, "Average episode: ${exactInfo.averageEpisodeRuntime} min")
            Log.d(TAG, "Completion: ${exactInfo.completionPercentage}%")

            return@withContext ExactRuntimeResult.Success(exactInfo, seasonsDetails)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get exact runtime for TV show $tvId: ${e.message}", e)
            return@withContext ExactRuntimeResult.Error("Помилка завантаження: ${e.message}")
        }
    }

    /**
     * Отримує спрощену інформацію тільки про перший сезон для швидкої оцінки
     */
    suspend fun getQuickRuntimeEstimate(tvId: Int): QuickEstimateResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting quick runtime estimate for TV show $tvId")

        try {
            Log.d(TAG, "Requesting season 1 details for TV show $tvId")
            val firstSeasonDetails = repository.getSeasonDetails(tvId, 1)
            Log.d(TAG, "Season details received: ${firstSeasonDetails.name}")

            val episodes = firstSeasonDetails.episodes ?: emptyList()
            Log.d(TAG, "Found ${episodes.size} episodes in season 1")

            if (episodes.isEmpty()) {
                Log.w(TAG, "No episodes found in season 1")
                return@withContext QuickEstimateResult.NoData
            }

            val episodeRuntimes = episodes.mapNotNull { episode ->
                Log.d(TAG, "Episode ${episode.episodeNumber}: runtime=${episode.runtime}")
                episode.runtime
            }.filter { it > 0 }

            Log.d(TAG, "Episodes with runtime data: ${episodeRuntimes.size} out of ${episodes.size}")

            if (episodeRuntimes.isEmpty()) {
                Log.w(TAG, "No runtime data found for any episodes")
                return@withContext QuickEstimateResult.NoRuntimeData
            }

            val averageRuntime = episodeRuntimes.average().toInt()
            val totalFirstSeasonMinutes = episodeRuntimes.sum()

            Log.d(TAG, "Quick estimate successful:")
            Log.d(TAG, "- Episodes in S1: ${episodes.size}")
            Log.d(TAG, "- With runtime: ${episodeRuntimes.size}")
            Log.d(TAG, "- Average runtime: ${averageRuntime} min")
            Log.d(TAG, "- Total S1 time: ${totalFirstSeasonMinutes} min")

            return@withContext QuickEstimateResult.Success(
                firstSeasonEpisodes = episodes.size,
                averageEpisodeRuntime = averageRuntime,
                firstSeasonTotalMinutes = totalFirstSeasonMinutes,
                episodesWithRuntime = episodeRuntimes.size
            )

        } catch (_: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout getting quick estimate for TV show $tvId")
            return@withContext QuickEstimateResult.Error("Таймаут API")
        } catch (_: java.net.UnknownHostException) {
            Log.e(TAG, "Network error getting quick estimate for TV show $tvId")
            return@withContext QuickEstimateResult.Error("Немає інтернету")
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error ${e.code()} getting quick estimate for TV show $tvId: ${e.message()}")
            return@withContext QuickEstimateResult.Error("HTTP ${e.code()}: ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quick estimate for TV show $tvId: ${e.message}", e)
            return@withContext QuickEstimateResult.Error(e.message ?: "Невідома помилка")
        }
    }

    sealed class ExactRuntimeResult {
        data class Success(
            val runtimeInfo: Utils.TvShowExactRuntimeInfo,
            val seasonsDetails: List<com.example.movietime.data.model.TvSeasonDetails>
        ) : ExactRuntimeResult()

        data class Error(val message: String) : ExactRuntimeResult()
    }

    sealed class QuickEstimateResult {
        data class Success(
            val firstSeasonEpisodes: Int,
            val averageEpisodeRuntime: Int,
            val firstSeasonTotalMinutes: Int,
            val episodesWithRuntime: Int
        ) : QuickEstimateResult()

        object NoData : QuickEstimateResult()
        object NoRuntimeData : QuickEstimateResult()
        data class Error(val message: String) : QuickEstimateResult()
    }
}
