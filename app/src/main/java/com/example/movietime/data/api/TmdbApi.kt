package com.example.movietime.data.api

import com.example.movietime.data.model.MoviesResponse
import com.example.movietime.data.model.TvShowsResponse
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.model.TvSeasonDetails
import com.example.movietime.data.model.TvEpisodeDetails
import com.example.movietime.data.model.GenresResponse
import com.example.movietime.data.model.PersonSearchResponse
import com.example.movietime.data.model.PersonDetails
import com.example.movietime.data.model.PersonMovieCredits
import com.example.movietime.data.model.PersonTvCredits
import com.example.movietime.data.model.CreditsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US"
    ): MoviesResponse

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US"
    ): TvShowsResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): MoviesResponse

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TvShowsResponse

    // Upcoming Movies (returns movies that are being released soon)
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("region") region: String = "UA",
        @Query("page") page: Int = 1
    ): MoviesResponse

    // TV Shows airing soon / on the air
    @GET("tv/on_the_air")
    suspend fun getOnTheAirTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1
    ): TvShowsResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String? = null
    ): MovieResult

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String? = null
    ): TvShowResult

    // Отримати детальну інформацію про конкретний сезон
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TvSeasonDetails

    // Отримати інформацію про конкретний епізод
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun getEpisodeDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Path("episode_number") episodeNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TvEpisodeDetails


    // Recommendations & Similar (Content-Based Filtering)

    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MoviesResponse

    @GET("tv/{tv_id}/recommendations")
    suspend fun getTvShowRecommendations(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TvShowsResponse

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MoviesResponse

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TvShowsResponse

    // ============ GENRES ============

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): GenresResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): GenresResponse

    // ============ DISCOVER (Advanced Filtering) ============

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") withGenres: String? = null,
        @Query("with_cast") withCast: String? = null,
        @Query("with_crew") withCrew: String? = null,
        @Query("with_people") withPeople: String? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_average.lte") voteAverageLte: Float? = null,
        @Query("primary_release_date.gte") releaseDateGte: String? = null,
        @Query("primary_release_date.lte") releaseDateLte: String? = null,
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("year") year: Int? = null
    ): MoviesResponse

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") withGenres: String? = null,
        @Query("with_cast") withCast: String? = null,
        @Query("with_crew") withCrew: String? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_average.lte") voteAverageLte: Float? = null,
        @Query("first_air_date.gte") firstAirDateGte: String? = null,
        @Query("first_air_date.lte") firstAirDateLte: String? = null,
        @Query("with_original_language") withOriginalLanguage: String? = null
    ): TvShowsResponse

    // ============ PEOPLE SEARCH ============

    @GET("search/person")
    suspend fun searchPeople(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1
    ): PersonSearchResponse

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): PersonDetails

    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovieCredits(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): PersonMovieCredits

    @GET("person/{person_id}/tv_credits")
    suspend fun getPersonTvCredits(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): PersonTvCredits

    // ============ MOVIE/TV CREDITS ============

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): CreditsResponse

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA"
    ): CreditsResponse

    // ============ POPULAR PEOPLE ============

    @GET("person/popular")
    suspend fun getPopularPeople(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "uk-UA",
        @Query("page") page: Int = 1
    ): PersonSearchResponse
}