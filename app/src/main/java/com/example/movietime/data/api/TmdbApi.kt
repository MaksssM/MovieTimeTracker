package com.example.movietime.data.api

import com.example.movietime.data.model.ApiMovieDetails
import com.example.movietime.data.model.ApiSearchResponse
import com.example.movietime.data.model.ApiTvShowDetails
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/multi")
    suspend fun search(
        @Query("query") query: String,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("language") language: String = "ru-RU",
        @Query("include_adult") includeAdult: Boolean = false
    ): ApiSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("language") language: String = "ru-RU"
    ): ApiMovieDetails

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("language") language: String = "ru-RU"
    ): ApiTvShowDetails

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        private const val API_KEY = "0292b01bfb3947a086ce90a47f04707d"

        fun create(): TmdbApi {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder().addInterceptor(logger).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TmdbApi::class.java)
        }
    }
}