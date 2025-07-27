package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchedItem(item: WatchedItem)

    @Query("SELECT * FROM watched_items_table ORDER BY title ASC")
    fun getAllWatchedItems(): LiveData<List<WatchedItem>>

    @Query("SELECT SUM(runtimeInMinutes) FROM watched_items_table")
    fun getTotalWatchTimeInMinutes(): LiveData<Int?>

    @Query("SELECT * FROM watched_items_table WHERE id = :id LIMIT 1")
    fun getWatchedItemById(id: Int): LiveData<WatchedItem?>

    @Query("DELETE FROM watched_items_table WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)
}