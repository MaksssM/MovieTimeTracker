package com.example.movietime.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowProgressDao {
    
    // Отримати весь прогрес для серіалу
    @Query("SELECT * FROM tv_show_progress WHERE tvShowId = :tvShowId ORDER BY seasonNumber, episodeNumber")
    fun getProgressForShow(tvShowId: Int): Flow<List<TvShowProgress>>
    
    @Query("SELECT * FROM tv_show_progress WHERE tvShowId = :tvShowId ORDER BY seasonNumber, episodeNumber")
    suspend fun getProgressForShowSync(tvShowId: Int): List<TvShowProgress>
    
    // Отримати прогрес для конкретного сезону
    @Query("SELECT * FROM tv_show_progress WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber ORDER BY episodeNumber")
    fun getProgressForSeason(tvShowId: Int, seasonNumber: Int): Flow<List<TvShowProgress>>
    
    @Query("SELECT * FROM tv_show_progress WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber ORDER BY episodeNumber")
    suspend fun getProgressForSeasonSync(tvShowId: Int, seasonNumber: Int): List<TvShowProgress>
    
    // Перевірити чи серія переглянута
    @Query("SELECT watched FROM tv_show_progress WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun isEpisodeWatched(tvShowId: Int, seasonNumber: Int, episodeNumber: Int): Boolean?
    
    // Кількість переглянутих серій
    @Query("SELECT COUNT(*) FROM tv_show_progress WHERE tvShowId = :tvShowId AND watched = 1")
    suspend fun getWatchedEpisodeCount(tvShowId: Int): Int
    
    // Кількість переглянутих серій в сезоні
    @Query("SELECT COUNT(*) FROM tv_show_progress WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber AND watched = 1")
    suspend fun getWatchedEpisodeCountInSeason(tvShowId: Int, seasonNumber: Int): Int
    
    // Загальна кількість серій для серіалу
    @Query("SELECT COUNT(*) FROM tv_show_progress WHERE tvShowId = :tvShowId")
    suspend fun getTotalEpisodeCount(tvShowId: Int): Int
    
    // Загальний час перегляду
    @Query("SELECT COALESCE(SUM(episodeRuntime), 0) FROM tv_show_progress WHERE tvShowId = :tvShowId AND watched = 1")
    suspend fun getWatchedRuntime(tvShowId: Int): Int
    
    // Загальний час всіх серій
    @Query("SELECT COALESCE(SUM(episodeRuntime), 0) FROM tv_show_progress WHERE tvShowId = :tvShowId")
    suspend fun getTotalRuntime(tvShowId: Int): Int
    
    // Вставити/оновити серію
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: TvShowProgress)
    
    // Вставити/оновити список серій
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<TvShowProgress>)
    
    // Позначити серію як переглянуту/непереглянуту
    @Query("UPDATE tv_show_progress SET watched = :watched, watchedAt = :watchedAt WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun setEpisodeWatched(tvShowId: Int, seasonNumber: Int, episodeNumber: Int, watched: Boolean, watchedAt: Long?)
    
    // Позначити весь сезон як переглянутий
    @Query("UPDATE tv_show_progress SET watched = :watched, watchedAt = :watchedAt WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber")
    suspend fun setSeasonWatched(tvShowId: Int, seasonNumber: Int, watched: Boolean, watchedAt: Long?)
    
    // Позначити весь серіал як переглянутий
    @Query("UPDATE tv_show_progress SET watched = :watched, watchedAt = :watchedAt WHERE tvShowId = :tvShowId")
    suspend fun setAllWatched(tvShowId: Int, watched: Boolean, watchedAt: Long?)
    
    // Видалити прогрес серіалу
    @Query("DELETE FROM tv_show_progress WHERE tvShowId = :tvShowId")
    suspend fun deleteProgressForShow(tvShowId: Int)
    
    // Видалити прогрес сезону
    @Query("DELETE FROM tv_show_progress WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber")
    suspend fun deleteProgressForSeason(tvShowId: Int, seasonNumber: Int)
    
    // Перевірити чи є якийсь прогрес для серіалу
    @Query("SELECT EXISTS(SELECT 1 FROM tv_show_progress WHERE tvShowId = :tvShowId LIMIT 1)")
    suspend fun hasProgressForShow(tvShowId: Int): Boolean
    
    // Отримати статистику по сезонах
    @Query("""
        SELECT 
            seasonNumber,
            COUNT(*) as totalEpisodes,
            SUM(CASE WHEN watched = 1 THEN 1 ELSE 0 END) as watchedEpisodes,
            COALESCE(SUM(episodeRuntime), 0) as totalRuntime,
            COALESCE(SUM(CASE WHEN watched = 1 THEN episodeRuntime ELSE 0 END), 0) as watchedRuntime
        FROM tv_show_progress 
        WHERE tvShowId = :tvShowId 
        GROUP BY seasonNumber 
        ORDER BY seasonNumber
    """)
    suspend fun getSeasonStats(tvShowId: Int): List<SeasonProgress>
}
