package com.example.movietime.di

import android.content.Context
import androidx.room.Room
import com.example.movietime.BuildConfig
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.AppDatabase
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.repository.AppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(retrofit: Retrofit): TmdbApi = retrofit.create(TmdbApi::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "movie_tracker_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }


    @Provides
    @Singleton
    fun provideWatchedItemDao(database: AppDatabase): WatchedItemDao {
        return database.watchedItemDao()
    }

    @Provides
    @Singleton
    fun provideAppRepository(api: TmdbApi, dao: WatchedItemDao): AppRepository {
        return AppRepository(api, dao, BuildConfig.TMDB_API_KEY)
    }
}