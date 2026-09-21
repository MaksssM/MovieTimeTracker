package com.example.movietime.di

import android.content.Context
import androidx.room.Room
import com.example.movietime.BuildConfig
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.*
import com.example.movietime.data.firebase.FirebaseRepository
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.data.repository.SimpleEnhancedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.movietime.util.LanguageManager
import javax.inject.Singleton

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import java.io.File
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val cacheDir = File(context.cacheDir, "tmdb_http_cache")
        val cacheSize = 50L * 1024 * 1024 // 50 MB
        val cache = Cache(cacheDir, cacheSize)

        // Cache successful responses for 30 minutes
        val onlineCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder()
                .maxAge(30, TimeUnit.MINUTES)
                .build()
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }

        // If offline, serve from cache up to 7 days
        val offlineCacheInterceptor = Interceptor { chain ->
            var request = chain.request()
            if (!isNetworkAvailable(context)) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .onlyIfCached()
                    .build()
                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(onlineCacheInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
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
    fun provideLanguageManager(@ApplicationContext context: Context): LanguageManager {
        return LanguageManager(context)
    }


    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "movie_tracker_database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideWatchedItemDao(database: AppDatabase): WatchedItemDao {
        return database.watchedItemDao()
    }

    @Provides
    @Singleton
    fun providePlannedDao(database: AppDatabase): PlannedDao {
        return database.plannedItemDao()
    }

    @Provides
    @Singleton
    fun provideWatchingDao(database: AppDatabase): WatchingDao {
        return database.watchingItemDao()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }
    
    @Provides
    @Singleton
    fun provideTvShowProgressDao(database: AppDatabase): TvShowProgressDao {
        return database.tvShowProgressDao()
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        api: TmdbApi, 
        dao: WatchedItemDao, 
        plannedDao: PlannedDao, 
        watchingDao: WatchingDao,
        tvShowProgressDao: TvShowProgressDao,
        searchHistoryDao: SearchHistoryDao,
        languageManager: LanguageManager
    ): AppRepository {
        if (BuildConfig.TMDB_API_KEY.isBlank() || BuildConfig.TMDB_API_KEY.contains("YOUR_DEFAULT_KEY")) {
            android.util.Log.e("AppModule", "WARNING: TMDB API key is not configured properly!")
        }
        return AppRepository(api, dao, plannedDao, watchingDao, tvShowProgressDao, searchHistoryDao, languageManager, BuildConfig.TMDB_API_KEY)
    }

    @Provides
    @Singleton
    fun provideSimpleEnhancedRepository(
        api: TmdbApi,
        appRepository: AppRepository
    ): SimpleEnhancedRepository {
        return SimpleEnhancedRepository(api, appRepository, BuildConfig.TMDB_API_KEY)
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }

    @Provides
    @Singleton
    fun provideUniverseDao(database: AppDatabase): UniverseDao {
        return database.universeDao()
    }
}