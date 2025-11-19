package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PlannedDao {
    @Query("SELECT * FROM planned_items ORDER BY dateAdded DESC")
    fun getAll(): LiveData<List<PlannedItem>>

    @Query("SELECT * FROM planned_items ORDER BY dateAdded DESC")
    suspend fun getAllSync(): List<PlannedItem>

    @Query("SELECT * FROM planned_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun getById(id: Int, mediaType: String): PlannedItem?

    @Query("SELECT * FROM planned_items WHERE mediaType = :mediaType ORDER BY dateAdded DESC")
    suspend fun getByMediaType(mediaType: String): List<PlannedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlannedItem)

    @Query("DELETE FROM planned_items WHERE id = :id AND mediaType = :mediaType")
    suspend fun deleteById(id: Int, mediaType: String)

    @Delete
    suspend fun delete(item: PlannedItem)

    @Query("DELETE FROM planned_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM planned_items")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM planned_items WHERE mediaType = :mediaType")
    suspend fun getCountByMediaType(mediaType: String): Int
}
