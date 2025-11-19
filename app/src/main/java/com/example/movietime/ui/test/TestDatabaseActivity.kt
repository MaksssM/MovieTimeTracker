package com.example.movietime.ui.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TestDatabaseActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: AppRepository

    private lateinit var tvStatus: TextView
    private lateinit var btnTest: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Створюємо простий layout програмно
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        tvStatus = TextView(this).apply {
            text = "Натисніть 'Тест' щоб додати фільм в БД"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }

        btnTest = Button(this).apply {
            text = "ТЕСТ: Додати Inception"
            setOnClickListener { testAddMovie() }
        }

        btnClear = Button(this).apply {
            text = "ОЧИСТИТИ БД"
            setOnClickListener { clearDatabase() }
        }

        layout.addView(tvStatus)
        layout.addView(btnTest)
        layout.addView(btnClear)

        setContentView(layout)

        // Показати поточний стан БД
        checkDatabase()
    }

    private fun testAddMovie() {
        tvStatus.text = "Тестування...\n"
        Log.d("TestDB", "=== START TEST ===")

        lifecycleScope.launch {
            try {
                // Створюємо тестовий фільм
                val testMovie = WatchedItem(
                    id = 27205,
                    title = "Inception",
                    posterPath = "/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",
                    releaseDate = "2010-07-16",
                    runtime = 148,
                    mediaType = "movie",
                    overview = "Dom Cobb is a skilled thief...",
                    voteAverage = 8.4,
                    userRating = 9.0f
                )

                appendStatus("Створено об'єкт WatchedItem:\n$testMovie\n")
                Log.d("TestDB", "Created: $testMovie")

                // Перевіряємо чи вже є
                val existing = repository.getWatchedItemById(27205, "movie")
                if (existing != null) {
                    appendStatus("⚠️ Фільм вже є в БД!\n")
                    Log.d("TestDB", "Already exists: $existing")
                } else {
                    appendStatus("✓ Фільм не знайдено в БД, додаємо...\n")
                    Log.d("TestDB", "Not found, inserting...")
                }

                // Додаємо в БД
                appendStatus("Викликаємо repository.addWatchedItem()...\n")
                repository.addWatchedItem(testMovie)
                appendStatus("✓ addWatchedItem() завершено без помилок\n")
                Log.d("TestDB", "addWatchedItem() completed")

                // Перевіряємо чи додалося
                val added = repository.getWatchedItemById(27205, "movie")
                if (added != null) {
                    appendStatus("✅ УСПІХ! Фільм знайдено в БД:\n$added\n")
                    Log.d("TestDB", "SUCCESS! Found: $added")
                } else {
                    appendStatus("❌ ПОМИЛКА! Фільм не знайдено після додавання!\n")
                    Log.e("TestDB", "FAILED! Not found after insert")
                }

                // Показуємо загальну кількість
                val count = repository.getWatchedItemsCount()
                appendStatus("\nЗагальна кількість в БД: $count\n")
                Log.d("TestDB", "Total count: $count")

                // Показуємо всі елементи
                val allItems = repository.getWatchedItemsSync()
                appendStatus("\nВсі елементи в БД:\n")
                allItems.forEach { item ->
                    appendStatus("- ${item.id}: ${item.title} (${item.mediaType})\n")
                    Log.d("TestDB", "Item: $item")
                }

                Log.d("TestDB", "=== END TEST ===")

            } catch (e: Exception) {
                appendStatus("❌ ВИНЯТОК: ${e.message}\n")
                Log.e("TestDB", "Exception", e)
                e.printStackTrace()
            }
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            try {
                val allItems = repository.getWatchedItemsSync()
                allItems.forEach { item ->
                    repository.deleteWatchedItem(item)
                }
                tvStatus.text = "БД очищено! Видалено ${allItems.size} елементів\n"
                Log.d("TestDB", "Database cleared, deleted ${allItems.size} items")
            } catch (e: Exception) {
                tvStatus.text = "Помилка очищення: ${e.message}\n"
                Log.e("TestDB", "Clear failed", e)
            }
        }
    }

    private fun checkDatabase() {
        lifecycleScope.launch {
            try {
                val count = repository.getWatchedItemsCount()
                val items = repository.getWatchedItemsSync()

                var status = "Поточний стан БД:\n"
                status += "Кількість елементів: $count\n\n"

                if (items.isEmpty()) {
                    status += "БД порожня\n"
                } else {
                    status += "Елементи:\n"
                    items.forEach { item ->
                        status += "- ${item.title} (${item.mediaType})\n"
                    }
                }

                tvStatus.text = status
                Log.d("TestDB", status)
            } catch (e: Exception) {
                tvStatus.text = "Помилка: ${e.message}\n"
                Log.e("TestDB", "Check failed", e)
            }
        }
    }

    private fun appendStatus(text: String) {
        runOnUiThread {
            tvStatus.append(text)
        }
    }
}

