package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

data class MoviesResponse(
    val results: List<ApiMovie>
)

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