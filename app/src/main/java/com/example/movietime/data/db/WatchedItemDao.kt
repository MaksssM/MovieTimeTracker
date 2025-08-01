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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchedItem)

    @Query("DELETE FROM watched_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM watched_items WHERE id = :id")
    suspend fun getById(id: Int): WatchedItem?
}