package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

// Existing API models that are referenced
data class MoviesResponse(
    val results: List<MovieResult>
)

data class TvShowsResponse(
    val results: List<TvShowResult>
)

data class MovieResult(
    val id: Int,
    val title: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList()
)

data class TvShowResult(
    val id: Int,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList()
)
