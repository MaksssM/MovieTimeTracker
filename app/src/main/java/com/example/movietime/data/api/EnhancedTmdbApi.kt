package com.example.movietime.data.api

import com.example.movietime.data.model.MovieSearchResponse
import com.example.movietime.data.model.TvSearchResponse
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EnhancedTmdbApi {

    // Enhanced search with filters
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA"
    ): Response<MultiSearchResponse>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA",
        @Query("year") year: Int? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null
    ): Response<MovieSearchResponse>

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("first_air_date_year") firstAirDateYear: Int? = null
    ): Response<TvSearchResponse>

    // Discover with advanced filters
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") withGenres: String? = null,
        @Query("without_genres") withoutGenres: String? = null,
        @Query("primary_release_date.gte") releaseDateGte: String? = null,
        @Query("primary_release_date.lte") releaseDateLte: String? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_average.lte") voteAverageLte: Float? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("with_runtime.gte") runtimeGte: Int? = null,
        @Query("with_runtime.lte") runtimeLte: Int? = null
    ): Response<MovieSearchResponse>

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") withGenres: String? = null,
        @Query("without_genres") withoutGenres: String? = null,
        @Query("air_date.gte") airDateGte: String? = null,
        @Query("air_date.lte") airDateLte: String? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_average.lte") voteAverageLte: Float? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null
    ): Response<TvSearchResponse>

    // Upcoming releases
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA"
    ): Response<MovieSearchResponse>

    @GET("tv/on_the_air")
    suspend fun getOnTheAirTvShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<TvSearchResponse>

    // Trending content
    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String, // all, movie, tv
        @Path("time_window") timeWindow: String, // day, week
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1
    ): Response<MultiSearchResponse>

    // Popular content
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA"
    ): Response<MovieSearchResponse>

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<TvSearchResponse>

    // Top rated content
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA"
    ): Response<MovieSearchResponse>

    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<TvSearchResponse>

    // Genres
    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): Response<GenresResponse>

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): Response<GenresResponse>

    // Detailed info
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("append_to_response") appendToResponse: String = "videos,credits,similar,recommendations"
    ): Response<MovieDetails>

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("append_to_response") appendToResponse: String = "videos,credits,similar,recommendations"
    ): Response<TvDetails>

    // Similar and recommendations
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<MovieSearchResponse>

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<TvSearchResponse>

    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<MovieSearchResponse>

    @GET("tv/{tv_id}/recommendations")
    suspend fun getTvRecommendations(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "uk-UA"
    ): Response<TvSearchResponse>
}

// Response models for API
data class MultiSearchResponse(
    val page: Int,
    val results: List<MultiSearchResult>,
    val total_pages: Int,
    val total_results: Int
)

data class MultiSearchResult(
    val id: Int,
    val media_type: String, // movie, tv, person
    val title: String? = null, // for movies
    val name: String? = null, // for tv shows
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null, // for movies
    val first_air_date: String? = null, // for tv shows
    val vote_average: Float = 0f,
    val vote_count: Int = 0,
    val popularity: Float = 0f,
    val genre_ids: List<Int> = emptyList()
)

data class GenresResponse(
    val genres: List<Genre>
)

data class Genre(
    val id: Int,
    val name: String
)

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    val popularity: Float,
    val genres: List<Genre>,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountry>,
    @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguage>,
    val videos: VideosResponse?,
    val credits: CreditsResponse?,
    val similar: MovieSearchResponse?,
    val recommendations: MovieSearchResponse?
)

data class TvDetails(
    val id: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    val popularity: Float,
    val genres: List<Genre>,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountry>,
    @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguage>,
    val videos: VideosResponse?,
    val credits: CreditsResponse?,
    val similar: TvSearchResponse?,
    val recommendations: TvSearchResponse?
)

data class ProductionCountry(
    val iso_3166_1: String,
    val name: String
)

data class SpokenLanguage(
    val english_name: String,
    val iso_639_1: String,
    val name: String
)

data class VideosResponse(
    val results: List<Video>
)

data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean
)

data class CreditsResponse(
    val cast: List<Cast>,
    val crew: List<Crew>
)

data class Cast(
    val id: Int,
    val name: String,
    val character: String,
    val profile_path: String?,
    val order: Int
)

data class Crew(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    val profile_path: String?
)
