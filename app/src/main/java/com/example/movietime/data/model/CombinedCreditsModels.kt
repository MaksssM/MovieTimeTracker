package com.example.movietime.data.model

import com.google.gson.annotations.SerializedName

data class PersonCombinedCredits(
    val id: Int,
    val cast: List<CombinedCastCredit>,
    val crew: List<CombinedCrewCredit>
)

data class CombinedCastCredit(
    val id: Int,
    val title: String?, // For movies
    val name: String?, // For TV shows
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("media_type") val mediaType: String, // "movie" or "tv"
    val character: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?, // Movie release date
    @SerializedName("first_air_date") val firstAirDate: String?, // TV show air date
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("episode_count") val episodeCount: Int = 0, // TV only
    @SerializedName("credit_id") val creditId: String? = null,
    val order: Int? = null // For movies
)

data class CombinedCrewCredit(
    val id: Int,
    val title: String?, // For movies
    val name: String?, // For TV shows
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("media_type") val mediaType: String, // "movie" or "tv"
    val job: String?,
    val department: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?, // Movie release date
    @SerializedName("first_air_date") val firstAirDate: String?, // TV show air date
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("episode_count") val episodeCount: Int = 0, // TV only
    @SerializedName("credit_id") val creditId: String? = null
)
