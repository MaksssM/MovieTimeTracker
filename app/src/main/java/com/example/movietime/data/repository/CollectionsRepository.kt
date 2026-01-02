package com.example.movietime.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.movietime.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user-created collections.
 * Like playlists in Spotify, users can create custom lists of movies/shows.
 */
@Singleton
class CollectionsRepository @Inject constructor(
    private val collectionDao: UserCollectionDao,
    private val collectionItemDao: CollectionItemDao
) {
    companion object {
        private const val TAG = "CollectionsRepository"
    }

    // ========== Collections CRUD ==========

    fun getAllCollections(): LiveData<List<UserCollection>> {
        return collectionDao.getAllCollections()
    }

    suspend fun getAllCollectionsSync(): List<UserCollection> {
        return collectionDao.getAllCollectionsSync()
    }

    suspend fun getCollectionById(id: Long): UserCollection? {
        return collectionDao.getCollectionById(id)
    }

    fun getCollectionByIdLive(id: Long): LiveData<UserCollection?> {
        return collectionDao.getCollectionByIdLive(id)
    }

    suspend fun createCollection(
        name: String,
        description: String? = null,
        emoji: String? = null,
        color: Int? = null
    ): Long {
        val collection = UserCollection(
            name = name,
            description = description,
            emoji = emoji,
            color = color,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = collectionDao.insert(collection)
        Log.d(TAG, "Created collection: $name with id $id")
        return id
    }

    suspend fun updateCollection(collection: UserCollection) {
        val updated = collection.copy(updatedAt = System.currentTimeMillis())
        collectionDao.update(updated)
        Log.d(TAG, "Updated collection: ${collection.name}")
    }

    suspend fun deleteCollection(id: Long) {
        collectionDao.deleteById(id)
        Log.d(TAG, "Deleted collection: $id")
    }

    // ========== Collection Items ==========

    fun getItemsForCollection(collectionId: Long): LiveData<List<CollectionItem>> {
        return collectionItemDao.getItemsForCollection(collectionId)
    }

    suspend fun getItemsForCollectionSync(collectionId: Long): List<CollectionItem> {
        return collectionItemDao.getItemsForCollectionSync(collectionId)
    }

    suspend fun addItemToCollection(
        collectionId: Long,
        itemId: Int,
        mediaType: String,
        title: String? = null,
        posterPath: String? = null
    ) {
        val item = CollectionItem(
            collectionId = collectionId,
            itemId = itemId,
            mediaType = mediaType,
            title = title,
            posterPath = posterPath,
            addedAt = System.currentTimeMillis()
        )
        collectionItemDao.insert(item)
        collectionDao.updateTimestamp(collectionId)
        Log.d(TAG, "Added $title to collection $collectionId")
    }

    suspend fun removeItemFromCollection(collectionId: Long, itemId: Int, mediaType: String) {
        collectionItemDao.deleteFromCollection(collectionId, itemId, mediaType)
        collectionDao.updateTimestamp(collectionId)
        Log.d(TAG, "Removed item $itemId from collection $collectionId")
    }

    suspend fun isItemInCollection(collectionId: Long, itemId: Int, mediaType: String): Boolean {
        return collectionItemDao.isItemInCollection(collectionId, itemId, mediaType)
    }

    suspend fun getCollectionsForItem(itemId: Int, mediaType: String): List<UserCollection> {
        return collectionItemDao.getCollectionsForItem(itemId, mediaType)
    }

    suspend fun getItemCountForCollection(collectionId: Long): Int {
        return collectionItemDao.getCountForCollection(collectionId)
    }

    suspend fun getPreviewPostersForCollection(collectionId: Long): List<String> {
        return collectionItemDao.getPreviewPosters(collectionId)
    }

    // ========== Collection with item count ==========

    suspend fun getCollectionsWithCounts(): List<CollectionWithCount> = withContext(Dispatchers.IO) {
        val collections = collectionDao.getAllCollectionsSync()
        collections.map { collection ->
            CollectionWithCount(
                collection = collection,
                itemCount = collectionItemDao.getCountForCollection(collection.id),
                previewPosters = collectionItemDao.getPreviewPosters(collection.id)
            )
        }
    }

    // ========== Bulk operations ==========

    suspend fun addItemToMultipleCollections(
        collectionIds: List<Long>,
        itemId: Int,
        mediaType: String,
        title: String? = null,
        posterPath: String? = null
    ) {
        val items = collectionIds.map { collectionId ->
            CollectionItem(
                collectionId = collectionId,
                itemId = itemId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                addedAt = System.currentTimeMillis()
            )
        }
        collectionItemDao.insertAll(items)
        collectionIds.forEach { collectionDao.updateTimestamp(it) }
        Log.d(TAG, "Added $title to ${collectionIds.size} collections")
    }

    suspend fun removeItemFromAllCollections(itemId: Int, mediaType: String) {
        val collections = collectionItemDao.getCollectionsContainingItem(itemId, mediaType)
        collections.forEach { item ->
            collectionItemDao.delete(item)
            collectionDao.updateTimestamp(item.collectionId)
        }
        Log.d(TAG, "Removed item $itemId from all collections")
    }
}

data class CollectionWithCount(
    val collection: UserCollection,
    val itemCount: Int,
    val previewPosters: List<String>
)
