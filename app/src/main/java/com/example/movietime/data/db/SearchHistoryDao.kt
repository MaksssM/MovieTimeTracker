package com.example.movietime.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryItem)
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SearchHistoryItem>>
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllSync(): List<SearchHistoryItem>
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SearchHistoryItem>
    
    @Query("DELETE FROM search_history WHERE id = :id AND mediaType = :mediaType")
    suspend fun delete(id: Int, mediaType: String)
    
    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getCount(): Int
}
