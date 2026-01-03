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
    @SerializedName("genre_ids") val genreIds: List<Int>? = null,
    // Details API returns genres array instead of genre_ids
    val genres: List<Genre>? = null
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
    @SerializedName("genre_ids") val genreIds: List<Int>? = null,
    // Details API returns genres array instead of genre_ids
    val genres: List<Genre>? = null,
    // Додаткові поля для кращого визначення статусу та часу
    val status: String? = null, // "Returning Series", "Ended", "In Production", "Canceled"
    @SerializedName("in_production") val inProduction: Boolean? = null,
    val seasons: List<TvSeason>? = null
)

data class TvSeason(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    val id: Int?,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int?
)

// Детальна інформація про сезон з епізодами
data class TvSeasonDetails(
    @SerializedName("air_date") val airDate: String?,
    val episodes: List<TvEpisodeDetails>?,
    val id: Int?,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int?
)

// Детальна інформація про епізод з тривалістю
data class TvEpisodeDetails(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    val id: Int?,
    val name: String?,
    val overview: String?,
    @SerializedName("runtime") val runtime: Int?, // Тривалість епізоду в хвилинах!
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("vote_count") val voteCount: Int?
)

// Company search
data class CompanyResult(
    val id: Int,
    val name: String?,
    @SerializedName("origin_country") val originCountry: String? = null,
    @SerializedName("logo_path") val logoPath: String? = null
)

data class CompanySearchResponse(
    val results: List<CompanyResult>
)


