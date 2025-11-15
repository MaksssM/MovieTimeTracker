package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

// API Response models for existing API
data class MovieSearchResponse(
    val page: Int,
    val results: List<Movie>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TvSearchResponse(
    val page: Int,
    val results: List<TvShow>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class Movie(
    val id: Int,
    val title: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    val popularity: Float,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    val genres: List<Genre>? = null
)

data class TvShow(
    val id: Int,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    val popularity: Float,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    val genres: List<Genre>? = null
)

data class Genre(
    val id: Int,
    val name: String
)

data class GenresResponse(
    val genres: List<Genre>
)

data class MultiSearchResponse(
    val page: Int,
    val results: List<MultiSearchResult>,
    @SerializedName("total_pages") val total_pages: Int,
    @SerializedName("total_results") val total_results: Int
)

data class MultiSearchResult(
    val id: Int,
    @SerializedName("media_type") val media_type: String, // "movie" or "tv"
    val title: String? = null, // для фільмів
    val name: String? = null, // для серіалів
    val overview: String?,
    @SerializedName("poster_path") val poster_path: String?,
    @SerializedName("backdrop_path") val backdrop_path: String?,
    @SerializedName("release_date") val release_date: String? = null, // для фільмів
    @SerializedName("first_air_date") val first_air_date: String? = null, // для серіалів
    @SerializedName("vote_average") val vote_average: Float = 0f,
    @SerializedName("vote_count") val vote_count: Int = 0,
    val popularity: Float = 0f,
    @SerializedName("genre_ids") val genre_ids: List<Int> = emptyList()
)
