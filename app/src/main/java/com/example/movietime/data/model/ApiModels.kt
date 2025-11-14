package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

data class MoviesResponse(
    val results: List<MovieResult>
)

data class TvShowsResponse(
    val results: List<TvShowResult>
)

data class MovieResult(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double?,
    @SerializedName("popularity")
    val popularity: Double?,
    val runtime: Int?,
    val mediaType: String = "movie"
)

data class TvSeasonResult(
    val season_number: Int,
    val name: String?,
    val episode_count: Int
)

data class TvShowResult(
    val id: Int,
    val name: String?,
    val title: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("first_air_date")
    val firstAirDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double?,
    @SerializedName("popularity")
    val popularity: Double?,
    @SerializedName("episode_run_time")
    val episodeRunTime: List<Int>?,
    @SerializedName("seasons")
    val seasons: List<TvSeasonResult>?,
    val mediaType: String = "tv"
) {
    val runtime: Int?
        get() = episodeRunTime?.firstOrNull()
}

sealed class SearchItem {
    abstract val id: Int
    abstract val title: String?
    abstract val name: String?
    abstract val posterPath: String?
    abstract val overview: String?
    abstract val releaseDate: String?
    abstract val voteAverage: Double?
    abstract val runtime: Int?
    abstract val mediaType: String
    
    @Suppress("unused")
    data class Movie(
        override val id: Int,
        override val title: String?,
        override val name: String?,
        @SerializedName("poster_path")
        override val posterPath: String?,
        @SerializedName("overview")
        override val overview: String?,
        @SerializedName("release_date")
        override val releaseDate: String?,
        @SerializedName("vote_average")
        override val voteAverage: Double?,
        override val runtime: Int?
    ) : SearchItem() {
        override val mediaType: String = "movie"
    }
    
    @Suppress("unused")
    data class TvShow(
        override val id: Int,
        override val name: String?,
        override val title: String?,
        @SerializedName("poster_path")
        override val posterPath: String?,
        @SerializedName("overview")
        override val overview: String?,
        @SerializedName("first_air_date")
        override val releaseDate: String?,
        @SerializedName("vote_average")
        override val voteAverage: Double?,
        @SerializedName("episode_run_time")
        val episodeRunTime: List<Int>?
    ) : SearchItem() {
        override val mediaType: String = "tv"
        override val runtime: Int? = episodeRunTime?.firstOrNull()
    }
}

// Deprecated - will be removed in favor of SearchItem
@Suppress("unused")
data class ApiMovie(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double?,
    val runtime: Int?
)

// Deprecated - will be removed in favor of SearchItem
@Suppress("unused")
data class ApiTvShow(
    val id: Int,
    val name: String?,
    val title: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("first_air_date")
    val firstAirDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double?,
    @SerializedName("episode_run_time")
    val episodeRunTime: List<Int>?
)