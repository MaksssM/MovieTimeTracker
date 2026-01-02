package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface YearlyStatsDao {
    
    @Query("SELECT * FROM yearly_stats WHERE year = :year")
    suspend fun getStatsForYear(year: Int): YearlyStats?
    
    @Query("SELECT * FROM yearly_stats WHERE year = :year")
    fun getStatsForYearLive(year: Int): LiveData<YearlyStats?>
    
    @Query("SELECT * FROM yearly_stats ORDER BY year DESC")
    fun getAllStats(): LiveData<List<YearlyStats>>
    
    @Query("SELECT * FROM yearly_stats ORDER BY year DESC")
    suspend fun getAllStatsSync(): List<YearlyStats>
    
    @Query("SELECT year FROM yearly_stats ORDER BY year DESC")
    suspend fun getAvailableYears(): List<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: YearlyStats)
    
    @Update
    suspend fun update(stats: YearlyStats)
    
    @Query("DELETE FROM yearly_stats WHERE year = :year")
    suspend fun deleteForYear(year: Int)
    
    @Query("SELECT EXISTS(SELECT 1 FROM yearly_stats WHERE year = :year)")
    suspend fun hasStatsForYear(year: Int): Boolean
    
    // Total lifetime stats
    @Query("SELECT SUM(totalWatchTimeMinutes) FROM yearly_stats")
    suspend fun getTotalLifetimeWatchTime(): Long?
    
    @Query("SELECT SUM(totalMovies) FROM yearly_stats")
    suspend fun getTotalLifetimeMovies(): Int?
    
    @Query("SELECT SUM(totalTvEpisodes) FROM yearly_stats")
    suspend fun getTotalLifetimeEpisodes(): Int?
}
