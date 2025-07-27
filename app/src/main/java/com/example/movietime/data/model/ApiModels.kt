package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

data class ApiSearchResponse(
    val results: List<ApiMediaItem>
)

data class ApiMediaItem(
    val id: Int,
    @SerializedName("media_type")
    val mediaType: String?, // "movie" или "tv"
    val title: String?,      // Для фильмов
    val name: String?,       // Для сериалов
    @SerializedName("poster_path")
    val posterPath: String?,
    val overview: String?
) {
    val universalTitle: String
        get() = title ?: name ?: "Без названия"
}

data class ApiMovieDetails(
    val id: Int,
    val title: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    val overview: String,
    val runtime: Int?
)

data class ApiTvShowDetails(
    val id: Int,
    val name: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    val overview: String,
    @SerializedName("episode_run_time")
    val episodeRunTime: List<Int>,
    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int?
)