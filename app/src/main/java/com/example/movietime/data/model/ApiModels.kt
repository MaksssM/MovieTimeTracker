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

// ============ PERSON MODELS ============

data class PersonSearchResponse(
    val page: Int,
    val results: List<Person>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class Person(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    val popularity: Float = 0f,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    @SerializedName("known_for") val knownFor: List<KnownForItem>? = null,
    val adult: Boolean = false,
    val gender: Int = 0 // 0 - not specified, 1 - female, 2 - male
)

data class KnownForItem(
    val id: Int,
    @SerializedName("media_type") val mediaType: String, // "movie" or "tv"
    val title: String? = null, // for movies
    val name: String? = null, // for tv shows
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f
)

data class PersonDetails(
    val id: Int,
    val name: String,
    val biography: String?,
    @SerializedName("profile_path") val profilePath: String?,
    val birthday: String?,
    val deathday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    val popularity: Float = 0f,
    @SerializedName("also_known_as") val alsoKnownAs: List<String> = emptyList(),
    val gender: Int = 0,
    val adult: Boolean = false,
    val homepage: String? = null,
    @SerializedName("imdb_id") val imdbId: String? = null
)

// ============ CREDITS MODELS ============

data class CreditsResponse(
    val id: Int,
    val cast: List<CastMember>,
    val crew: List<CrewMember>
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    val order: Int = 0,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Float = 0f,
    @SerializedName("cast_id") val castId: Int? = null,
    @SerializedName("credit_id") val creditId: String? = null,
    val gender: Int = 0,
    val adult: Boolean = false
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val department: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Float = 0f,
    @SerializedName("credit_id") val creditId: String? = null,
    val gender: Int = 0,
    val adult: Boolean = false
)

// ============ PERSON CREDITS ============

data class PersonMovieCredits(
    val id: Int,
    val cast: List<PersonMovieCastCredit>,
    val crew: List<PersonMovieCrewCredit>
)

data class PersonMovieCastCredit(
    val id: Int,
    val title: String?,
    @SerializedName("original_title") val originalTitle: String?,
    val character: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("credit_id") val creditId: String? = null,
    val order: Int? = null
)

data class PersonMovieCrewCredit(
    val id: Int,
    val title: String?,
    @SerializedName("original_title") val originalTitle: String?,
    val job: String?,
    val department: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("credit_id") val creditId: String? = null
)

data class PersonTvCredits(
    val id: Int,
    val cast: List<PersonTvCastCredit>,
    val crew: List<PersonTvCrewCredit>
)

data class PersonTvCastCredit(
    val id: Int,
    val name: String?,
    @SerializedName("original_name") val originalName: String?,
    val character: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("credit_id") val creditId: String? = null
)

data class PersonTvCrewCredit(
    val id: Int,
    val name: String?,
    @SerializedName("original_name") val originalName: String?,
    val job: String?,
    val department: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val popularity: Float = 0f,
    val overview: String?,
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("credit_id") val creditId: String? = null
)

// ============ SEARCH FILTER DATA ============

data class SearchFilter(
    val genres: List<Genre> = emptyList(),
    val selectedPerson: Person? = null,
    val personRole: PersonRole = PersonRole.ANY,
    val minRating: Float = 0f,
    val maxRating: Float = 10f,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val sortBy: SortOption = SortOption.POPULARITY_DESC
)

enum class PersonRole {
    ANY,        // Any role (actor or director)
    ACTOR,      // Only as cast member
    DIRECTOR,   // Only as director
    WRITER,     // Only as writer
    PRODUCER    // Only as producer
}

enum class SortOption(val apiValue: String, val displayName: String) {
    POPULARITY_DESC("popularity.desc", "Популярність ↓"),
    POPULARITY_ASC("popularity.asc", "Популярність ↑"),
    VOTE_AVERAGE_DESC("vote_average.desc", "Рейтинг ↓"),
    VOTE_AVERAGE_ASC("vote_average.asc", "Рейтинг ↑"),
    RELEASE_DATE_DESC("primary_release_date.desc", "Дата виходу ↓"),
    RELEASE_DATE_ASC("primary_release_date.asc", "Дата виходу ↑"),
    TITLE_ASC("title.asc", "Назва А-Я"),
    TITLE_DESC("title.desc", "Назва Я-А")
}

// ============ TV SEASON & EPISODE DETAILS ============

data class SeasonDetails(
    val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("air_date") val airDate: String?,
    val episodes: List<EpisodeDetails>?
)

data class EpisodeDetails(
    val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String?,
    val overview: String?,
    @SerializedName("air_date") val airDate: String?,
    val runtime: Int?, // Тривалість серії в хвилинах
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Float = 0f,
    @SerializedName("vote_count") val voteCount: Int = 0
)
