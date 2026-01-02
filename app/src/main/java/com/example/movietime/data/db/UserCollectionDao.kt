package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserCollectionDao {
    
    @Query("SELECT * FROM user_collections ORDER BY updatedAt DESC")
    fun getAllCollections(): LiveData<List<UserCollection>>
    
    @Query("SELECT * FROM user_collections ORDER BY updatedAt DESC")
    suspend fun getAllCollectionsSync(): List<UserCollection>
    
    @Query("SELECT * FROM user_collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): UserCollection?
    
    @Query("SELECT * FROM user_collections WHERE id = :id")
    fun getCollectionByIdLive(id: Long): LiveData<UserCollection?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: UserCollection): Long
    
    @Update
    suspend fun update(collection: UserCollection)
    
    @Delete
    suspend fun delete(collection: UserCollection)
    
    @Query("DELETE FROM user_collections WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM user_collections")
    suspend fun getCount(): Int
    
    @Query("UPDATE user_collections SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())
}
