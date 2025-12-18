package com.example.movietime.data.migration

import android.util.Log
import com.example.movietime.data.db.WatchedItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Утиліта для виправлення runtime серіалів
 * Використовується для перерахунку runtime серіалів, які мають null або 0
 */
class TvShowRuntimeFixer @Inject constructor(
    private val watchedItemDao: WatchedItemDao
) {

    companion object {
        private const val TAG = "TvShowRuntimeFixer"
    }

    /**
     * Виправляє всі серіали з неправильним runtime
     */
    suspend fun fixAllTvShowRuntimes() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting TV show runtime fix...")

        val allItems = watchedItemDao.getAllSync()
        val tvShows = allItems.filter { it.mediaType == "tv" }

        Log.d(TAG, "Found ${tvShows.size} TV shows to check")

        var fixedCount = 0
        var skippedCount = 0

        for (show in tvShows) {
            val needsFix = show.runtime == null || show.runtime == 0

            if (needsFix) {
                // Перераховуємо runtime
                val episodes = show.totalEpisodes ?: 0
                val episodeRuntime = show.episodeRuntime ?: estimateEpisodeRuntime(show.title)
                val calculatedRuntime = episodes * episodeRuntime

                if (calculatedRuntime > 0) {
                    // Оновлюємо запис
                    val updatedShow = show.copy(
                        runtime = calculatedRuntime,
                        episodeRuntime = if (show.episodeRuntime == null) episodeRuntime else show.episodeRuntime
                    )

                    watchedItemDao.insert(updatedShow)

                    Log.d(TAG, "✅ Fixed: ${show.title}")
                    Log.d(TAG, "   Old runtime: ${show.runtime}")
                    Log.d(TAG, "   New runtime: $calculatedRuntime ($episodes eps × $episodeRuntime min)")

                    fixedCount++
                } else {
                    Log.w(TAG, "⚠️ Cannot fix ${show.title}: insufficient data (episodes=$episodes, episodeRuntime=$episodeRuntime)")
                    skippedCount++
                }
            } else {
                Log.d(TAG, "✓ OK: ${show.title} (runtime=${show.runtime})")
            }
        }

        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "TV show runtime fix completed!")
        Log.d(TAG, "Fixed: $fixedCount")
        Log.d(TAG, "Skipped: $skippedCount")
        Log.d(TAG, "Total checked: ${tvShows.size}")
        Log.d(TAG, "=".repeat(50))

        TvShowFixResult(
            totalChecked = tvShows.size,
            fixed = fixedCount,
            skipped = skippedCount
        )
    }

    /**
     * Оцінює тривалість епізоду на основі назви серіалу
     */
    private fun estimateEpisodeRuntime(title: String): Int {
        val lowercaseTitle = title.lowercase()

        return when {
            // Комедійні серіали зазвичай 20-25 хв
            lowercaseTitle.contains("friends") -> 22  // Друзі
            lowercaseTitle.contains("big bang") -> 22 // Теорія великого вибуху
            lowercaseTitle.contains("office") -> 22   // Офіс
            lowercaseTitle.contains("modern family") -> 22
            lowercaseTitle.contains("brooklyn") -> 22 // Бруклін 9-9
            lowercaseTitle.contains("how i met") -> 22 // Як я зустрів вашу маму

            // Драматичні серіали зазвичай 40-60 хв
            lowercaseTitle.contains("breaking bad") -> 47
            lowercaseTitle.contains("game of thrones") -> 59
            lowercaseTitle.contains("stranger things") -> 51
            lowercaseTitle.contains("house") -> 44
            lowercaseTitle.contains("lost") -> 42
            lowercaseTitle.contains("dexter") -> 53

            // Аніме зазвичай 24 хв
            lowercaseTitle.contains("naruto") -> 24
            lowercaseTitle.contains("one piece") -> 24
            lowercaseTitle.contains("attack on titan") -> 24

            // Дефолтне значення
            else -> 45
        }
    }

    data class TvShowFixResult(
        val totalChecked: Int,
        val fixed: Int,
        val skipped: Int
    )
}

