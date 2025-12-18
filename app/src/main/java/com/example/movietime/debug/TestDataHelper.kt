package com.example.movietime.debug

import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Утиліта для додавання тестових даних у базу даних
 * Використовується лише для тестування та демонстрації
 */
class TestDataHelper @Inject constructor(
    private val watchedItemDao: WatchedItemDao
) {

    /**
     * Додає тестові фільми та серіали в базу даних
     */
    suspend fun addTestData() = withContext(Dispatchers.IO) {
        val testItems = listOf(
            // Фільми
            WatchedItem(
                id = 1396, // Breaking Bad (але як фільм для тесту)
                title = "Breaking Bad",
                posterPath = "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
                releaseDate = "2008-01-20",
                mediaType = "movie",
                runtime = 142,
                userRating = 5.0f
            ),
            WatchedItem(
                id = 550, // Fight Club
                title = "Fight Club",
                posterPath = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                releaseDate = "1999-10-15",
                mediaType = "movie",
                runtime = 139,
                userRating = 5.0f
            ),
            WatchedItem(
                id = 680, // Pulp Fiction
                title = "Pulp Fiction",
                posterPath = "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
                releaseDate = "1994-09-10",
                mediaType = "movie",
                runtime = 154,
                userRating = 5.0f
            ),
            // Серіали
            WatchedItem(
                id = 1399, // Game of Thrones
                title = "Game of Thrones",
                posterPath = "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                releaseDate = "2011-04-17",
                mediaType = "tv",
                runtime = 4307, // 73 епізоди × 59 хв
                userRating = 4.5f
            )
        )

        testItems.forEach { item ->
            watchedItemDao.insert(item)
            android.util.Log.d("TestDataHelper", "Added: ${item.title} (${item.runtime} min)")
        }

        val count = watchedItemDao.getCount()
        android.util.Log.d("TestDataHelper", "Total items in DB: $count")
    }

    /**
     * Очищає всі тестові дані
     */
    @Suppress("unused")
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        // Видаляємо тестові елементи
        val testIds = listOf(1396, 550, 680, 1399)
        testIds.forEach { id ->
            watchedItemDao.deleteById(id, "movie")
            watchedItemDao.deleteById(id, "tv")
        }

        val count = watchedItemDao.getCount()
        android.util.Log.d("TestDataHelper", "Remaining items in DB: $count")
    }
}

