package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RewatchDao {
    
    @Query("SELECT * FROM rewatch_entries ORDER BY watchedAt DESC")
    fun getAllRewatches(): LiveData<List<RewatchEntry>>
    
    @Query("SELECT * FROM rewatch_entries ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecentRewatches(limit: Int): List<RewatchEntry>
    
    @Query("SELECT * FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType ORDER BY watchedAt DESC")
    fun getRewatchesForItem(itemId: Int, mediaType: String): LiveData<List<RewatchEntry>>
    
    @Query("SELECT * FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType ORDER BY watchedAt DESC")
    suspend fun getRewatchesForItemSync(itemId: Int, mediaType: String): List<RewatchEntry>
    
    @Query("SELECT COUNT(*) FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType")
    suspend fun getRewatchCount(itemId: Int, mediaType: String): Int
    
    @Query("SELECT COUNT(*) FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType")
    fun getRewatchCountLive(itemId: Int, mediaType: String): LiveData<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RewatchEntry): Long
    
    @Delete
    suspend fun delete(entry: RewatchEntry)
    
    @Query("DELETE FROM rewatch_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType")
    suspend fun deleteAllForItem(itemId: Int, mediaType: String)
    
    // Statistics queries
    @Query("SELECT SUM(watchTimeMinutes) FROM rewatch_entries WHERE watchedAt BETWEEN :startTime AND :endTime")
    suspend fun getTotalWatchTime(startTime: Long, endTime: Long): Long?
    
    @Query("SELECT COUNT(*) FROM rewatch_entries WHERE watchedAt BETWEEN :startTime AND :endTime")
    suspend fun getWatchCount(startTime: Long, endTime: Long): Int
    
    @Query("SELECT * FROM rewatch_entries WHERE watchedAt BETWEEN :startTime AND :endTime ORDER BY watchedAt DESC")
    suspend fun getRewatchesInPeriod(startTime: Long, endTime: Long): List<RewatchEntry>
    
    // Most rewatched item
    @Query("""
        SELECT itemId, mediaType, title, COUNT(*) as count 
        FROM rewatch_entries 
        WHERE watchedAt BETWEEN :startTime AND :endTime
        GROUP BY itemId, mediaType 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getMostRewatched(startTime: Long, endTime: Long): MostRewatchedResult?
    
    @Query("SELECT AVG(userRating) FROM rewatch_entries WHERE itemId = :itemId AND mediaType = :mediaType AND userRating IS NOT NULL")
    suspend fun getAverageRating(itemId: Int, mediaType: String): Float?
}

data class MostRewatchedResult(
    val itemId: Int,
    val mediaType: String,
    val title: String?,
    val count: Int
)
