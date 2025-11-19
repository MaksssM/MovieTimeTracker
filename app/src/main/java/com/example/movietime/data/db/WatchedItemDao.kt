package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WatchedItemDao {

    @Query("SELECT * FROM watched_items")
    fun getAll(): LiveData<List<WatchedItem>>

    @Query("SELECT * FROM watched_items")
    suspend fun getAllSync(): List<WatchedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchedItem)

    @Query("DELETE FROM watched_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun deleteById(id: Int, mediaType: String)

    @Query("SELECT * FROM watched_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun getById(id: Int, mediaType: String): WatchedItem?

    @Query("SELECT COUNT(*) FROM watched_items")
    suspend fun getCount(): Int
}