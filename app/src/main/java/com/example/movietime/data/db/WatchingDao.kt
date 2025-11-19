package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchingDao {
    @Query("SELECT * FROM watching_items ORDER BY dateAdded DESC")
    fun getAll(): LiveData<List<WatchingItem>>

    @Query("SELECT * FROM watching_items ORDER BY dateAdded DESC")
    suspend fun getAllSync(): List<WatchingItem>

    @Query("SELECT * FROM watching_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun getById(id: Int, mediaType: String): WatchingItem?

    @Query("SELECT * FROM watching_items WHERE mediaType = :mediaType ORDER BY dateAdded DESC")
    suspend fun getByMediaType(mediaType: String): List<WatchingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchingItem)

    @Query("DELETE FROM watching_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun deleteById(id: Int, mediaType: String)

    @Delete
    suspend fun delete(item: WatchingItem)

    @Query("DELETE FROM watching_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM watching_items")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM watching_items WHERE mediaType = :mediaType")
    suspend fun getCountByMediaType(mediaType: String): Int
}
