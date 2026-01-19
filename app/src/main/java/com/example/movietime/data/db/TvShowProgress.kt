package com.example.movietime.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Зберігає прогрес перегляду серіалів по окремих серіях
 */
@Entity(
    tableName = "tv_show_progress",
    primaryKeys = ["tvShowId", "seasonNumber", "episodeNumber"],
    indices = [
        Index(value = ["tvShowId"]),
        Index(value = ["watched"]),
        Index(value = ["watchedAt"])
    ]
)
data class TvShowProgress(
    val tvShowId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String? = null,
    val episodeRuntime: Int? = null, // Тривалість серії в хвилинах
    val watched: Boolean = false,
    val watchedAt: Long? = null // Коли переглянуто
)

/**
 * Допоміжний клас для відображення статистики по сезону
 */
data class SeasonProgress(
    val seasonNumber: Int,
    val totalEpisodes: Int,
    val watchedEpisodes: Int,
    val totalRuntime: Int, // Загальна тривалість сезону
    val watchedRuntime: Int // Переглянутий час
) {
    val isComplete: Boolean get() = watchedEpisodes == totalEpisodes
    val progressPercent: Float get() = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f
}

/**
 * Допоміжний клас для відображення загального прогресу серіалу
 */
data class TvShowProgressSummary(
    val tvShowId: Int,
    val totalSeasons: Int,
    val totalEpisodes: Int,
    val watchedEpisodes: Int,
    val totalRuntime: Int,
    val watchedRuntime: Int,
    val seasons: List<SeasonProgress>
) {
    val isComplete: Boolean get() = watchedEpisodes == totalEpisodes
    val progressPercent: Float get() = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f
}
