package com.example.movietime.ui.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.movietime.R
import com.example.movietime.databinding.DrawerlayoutBinding
import com.example.movietime.service.TvShowUpdateService
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.migration.TvShowRuntimeFixer
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: DrawerlayoutBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var tvShowUpdateService: TvShowUpdateService

    @Inject
    lateinit var watchedItemDao: WatchedItemDao

    companion object {
        private const val TAG = "MainActivity"
        private var toolbarClickCount = 0
        private var lastClickTime = 0L
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "uk") ?: "uk"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("uk")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DrawerlayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Визначаємо top-level destinations - на них буде іконка меню замість стрілки назад
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.enhancedMainFragment, R.id.watchedFragment, R.id.trendingFragment, R.id.settingsFragment
            ), binding.drawerLayout
        )

        // Налаштовуємо toolbar з навігацією
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Налаштовуємо NavigationView
        binding.navView.setupWithNavController(navController)

        // Закриваємо drawer після вибору пункту меню
        // Закриваємо drawer після вибору пункту меню
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            
            when (menuItem.itemId) {
                R.id.nav_planned -> {
                    startActivity(android.content.Intent(this, com.example.movietime.ui.planned.PlannedActivity::class.java))
                    true
                }
                R.id.nav_watching -> {
                    startActivity(android.content.Intent(this, com.example.movietime.ui.watching.WatchingActivity::class.java))
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    true
                }
            }
        }

        // Запускаємо оновлення серіалів у фоновому режимі
        startTvShowUpdates()

        // Додаємо можливість додавати тестові дані через довгий клік на тулбар (для розробки)
        // Подвійний довгий клік - виправлення runtime серіалів
        binding.toolbar.setOnLongClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime < 2000) {
                // Подвійний клік протягом 2 секунд - виправлення runtime
                toolbarClickCount++
                if (toolbarClickCount >= 2) {
                    toolbarClickCount = 0
                    fixTvShowRuntimes()
                }
            } else {
                // Перший клік - тестові дані
                toolbarClickCount = 1
                addTestData()
            }

            lastClickTime = currentTime
            true
        }
    }

    private fun startTvShowUpdates() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting TV show updates check")
                val result = tvShowUpdateService.updateOngoingTvShows()

                if (result.hasUpdates) {
                    Log.d(TAG, "Updated ${result.updatedCount} TV shows")
                    // Можна показати Toast або Snackbar про оновлення
                    // Snackbar.make(binding.root, "Оновлено ${result.updatedCount} серіалів", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update TV shows: ${e.message}")
            }
        }
    }

    /**
     * Додає тестові дані для демонстрації (видалено - більше не доступно)
     */
    private fun addTestData() {
        try {
            Log.d(TAG, "Test data function deprecated - use real app data instead")

            Snackbar.make(
                binding.root,
                "Тестові дані більше не доступні. Використовуйте додаток як звичайно.",
                Snackbar.LENGTH_LONG
            ).show()

            Log.d(TAG, "Test data function called but disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed: ${e.message}", e)
        }
    }

    /**
     * Виправляє runtime для всіх серіалів
     * Викликається через подвійний довгий клік на тулбар
     */
    private fun fixTvShowRuntimes() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fixing TV show runtimes...")
                val fixer = TvShowRuntimeFixer(watchedItemDao)
                val result = fixer.fixAllTvShowRuntimes()

                val message = "Виправлено runtime серіалів:\n" +
                        "✅ Перевірено: ${result.totalChecked}\n" +
                        "✅ Виправлено: ${result.fixed}\n" +
                        "⏭️ Пропущено: ${result.skipped}"

                Snackbar.make(
                    binding.root,
                    message,
                    Snackbar.LENGTH_LONG
                ).show()

                Log.d(TAG, "TV show runtimes fixed successfully: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fix TV show runtimes: ${e.message}", e)
                Snackbar.make(
                    binding.root,
                    "Помилка виправлення: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}