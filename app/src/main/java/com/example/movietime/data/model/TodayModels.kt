package com.example.movietime.data.model

/**
 * Models for the "Today" digest screen.
 * Contains new episodes, releases, and news.
 */

/**
 * Main container for all Today screen data
 */
data class TodayDigest(
    val greeting: String = "",
    val date: String = "",
    val newEpisodes: List<NewEpisodeItem> = emptyList(),
    val todayReleases: List<TodayReleaseItem> = emptyList(),
    val upcomingThisWeek: List<TodayReleaseItem> = emptyList(),
    val trendingNews: List<NewsItem> = emptyList(),
    val personalTips: List<PersonalTip> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList()
)

/**
 * New episode that aired today or recently for shows user is watching
 */
data class NewEpisodeItem(
    val tvShowId: Int,
    val tvShowName: String,
    val posterPath: String?,
    val backdropPath: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String?,
    val airDate: String?,
    val overview: String?,
    val runtime: Int?,
    val voteAverage: Float = 0f,
    val isWatched: Boolean = false
)

/**
 * Movie or TV show releasing today
 */
data class TodayReleaseItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val mediaType: String, // "movie" or "tv"
    val voteAverage: Float = 0f,
    val overview: String?,
    val genres: List<String> = emptyList(),
    val isInWatchlist: Boolean = false
)

/**
 * News item related to movies/TV shows
 */
data class NewsItem(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val source: String?,
    val publishedAt: String?,
    val relatedMovieId: Int? = null,
    val relatedTvShowId: Int? = null,
    val newsType: NewsType = NewsType.GENERAL
)

enum class NewsType {
    GENERAL,
    TRAILER,
    CASTING,
    AWARD,
    REVIEW,
    ANNOUNCEMENT
}

/**
 * Personal tip/recommendation for the user
 */
data class PersonalTip(
    val id: String,
    val type: TipType,
    val title: String,
    val description: String,
    val iconResId: Int? = null,
    val actionText: String? = null,
    val relatedItemId: Int? = null,
    val relatedItemType: String? = null
)

enum class TipType {
    CONTINUE_WATCHING,
    NEW_SEASON_AVAILABLE,
    SIMILAR_CONTENT,
    MILESTONE_REACHED,
    UPCOMING_REMINDER,
    REWATCH_SUGGESTION
}

/**
 * Item for "Continue Watching" section
 */
data class ContinueWatchingItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: String,
    val progress: Float, // 0.0 to 1.0
    val lastWatchedEpisode: String?, // "S2E5" format for TV
    val nextEpisode: String?, // "S2E6" format for TV
    val totalEpisodes: Int? = null,
    val watchedEpisodes: Int? = null
)
