package com.example.movietime.util

import android.content.Context
import androidx.room.Room
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.AppDatabase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Utils {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "movie_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }

    fun formatMinutesToHoursAndMinutes(minutes: Int?): String {
        if (minutes == null || minutes < 0) {
            return "0 год 0 хв"
        }
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours} год ${remainingMinutes} хв"
    }
}