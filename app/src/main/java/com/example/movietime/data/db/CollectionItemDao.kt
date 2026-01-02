package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CollectionItemDao {
    
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun getItemsForCollection(collectionId: Long): LiveData<List<CollectionItem>>
    
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    suspend fun getItemsForCollectionSync(collectionId: Long): List<CollectionItem>
    
    @Query("SELECT * FROM collection_items WHERE itemId = :itemId AND mediaType = :mediaType")
    suspend fun getCollectionsContainingItem(itemId: Int, mediaType: String): List<CollectionItem>
    
    @Query("SELECT c.* FROM user_collections c INNER JOIN collection_items ci ON c.id = ci.collectionId WHERE ci.itemId = :itemId AND ci.mediaType = :mediaType")
    suspend fun getCollectionsForItem(itemId: Int, mediaType: String): List<UserCollection>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CollectionItem>)
    
    @Delete
    suspend fun delete(item: CollectionItem)
    
    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND itemId = :itemId AND mediaType = :mediaType")
    suspend fun deleteFromCollection(collectionId: Long, itemId: Int, mediaType: String)
    
    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId")
    suspend fun deleteAllFromCollection(collectionId: Long)
    
    @Query("SELECT COUNT(*) FROM collection_items WHERE collectionId = :collectionId")
    suspend fun getCountForCollection(collectionId: Long): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM collection_items WHERE collectionId = :collectionId AND itemId = :itemId AND mediaType = :mediaType)")
    suspend fun isItemInCollection(collectionId: Long, itemId: Int, mediaType: String): Boolean
    
    // Get first 4 poster paths for collection cover preview
    @Query("SELECT posterPath FROM collection_items WHERE collectionId = :collectionId AND posterPath IS NOT NULL ORDER BY addedAt DESC LIMIT 4")
    suspend fun getPreviewPosters(collectionId: Long): List<String>
}
