package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WatchedItemDao {

    @Query("SELECT * FROM watched_items")
    fun getAll(): LiveData<List<WatchedItem>>

    @Query("SELECT * FROM watched_items")
    suspend fun getAllSync(): List<WatchedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchedItem)

    @Update
    suspend fun update(item: WatchedItem)

    @Query("DELETE FROM watched_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun deleteById(id: Int, mediaType: String)

    @Query("DELETE FROM watched_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM watched_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun getById(id: Int, mediaType: String): WatchedItem?

    @Query("SELECT * FROM watched_items WHERE id = :id AND mediaType = :mediaType LIMIT 1")
    suspend fun getWatchedItem(id: Int, mediaType: String): WatchedItem?

    @Query("SELECT COUNT(*) FROM watched_items")
    suspend fun getCount(): Int

    @Query("UPDATE watched_items SET watchCount = watchCount + 1 WHERE id = :id AND mediaType = :mediaType")
    suspend fun incrementWatchCount(id: Int, mediaType: String)

    @Query("SELECT watchCount FROM watched_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun getWatchCount(id: Int, mediaType: String): Int

    @Query("SELECT id, mediaType FROM watched_items")
    suspend fun getAllIds(): List<MediaId>
}