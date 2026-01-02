package com.example.movietime.di

import com.example.movietime.data.db.AppDatabase
import com.example.movietime.data.db.UserCollectionDao
import com.example.movietime.data.db.CollectionItemDao
import com.example.movietime.data.db.RewatchDao
import com.example.movietime.data.db.FollowedPersonDao
import com.example.movietime.data.db.YearlyStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    
    @Provides
    @Singleton
    fun provideUserCollectionDao(database: AppDatabase): UserCollectionDao {
        return database.userCollectionDao()
    }
    
    @Provides
    @Singleton
    fun provideCollectionItemDao(database: AppDatabase): CollectionItemDao {
        return database.collectionItemDao()
    }
    
    @Provides
    @Singleton
    fun provideRewatchDao(database: AppDatabase): RewatchDao {
        return database.rewatchDao()
    }
    
    @Provides
    @Singleton
    fun provideFollowedPersonDao(database: AppDatabase): FollowedPersonDao {
        return database.followedPersonDao()
    }
    
    @Provides
    @Singleton
    fun provideYearlyStatsDao(database: AppDatabase): YearlyStatsDao {
        return database.yearlyStatsDao()
    }
}
