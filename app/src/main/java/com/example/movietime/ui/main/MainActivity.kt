package com.example.movietime.ui.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.view.MenuItem
import android.view.WindowManager
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
import com.example.movietime.ui.today.TodayActivity
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
        
        // Увімкнення blur background для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 40
        }
        
        // Увімкнення shared element transitions
        window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
        
        binding = DrawerlayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Визначаємо top-level destinations - на них буде іконка меню замість стрілки назад
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.enhancedMainFragment, R.id.watchedFragment, R.id.trendingFragment, R.id.settingsFragment, R.id.calendarFragment
            ), binding.drawerLayout
        )

        // Налаштовуємо toolbar з навігацією
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Налаштовуємо NavigationView з навігацією
        binding.navView.setupWithNavController(navController)

        // Додаємо обробник для Activity (не Fragment)
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            Log.d(TAG, "Menu item clicked: ${menuItem.itemId}, title: ${menuItem.title}")
            when (menuItem.itemId) {
                R.id.nav_today -> {
                    Log.d(TAG, "Opening TodayActivity")
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(android.content.Intent(this, TodayActivity::class.java))
                    true
                }
                R.id.nav_planned -> {
                    Log.d(TAG, "Opening PlannedActivity")
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(android.content.Intent(this, com.example.movietime.ui.planned.PlannedActivity::class.java))
                    true
                }
                R.id.nav_watching -> {
                    Log.d(TAG, "Opening WatchingActivity")
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(android.content.Intent(this, com.example.movietime.ui.watching.WatchingActivity::class.java))
                    true
                }
                R.id.enhancedMainFragment -> {
                    Log.d(TAG, "Navigating to Main Menu (Explicit)")
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    // Clear back stack back to home or navigate if not present
                    if (!navController.popBackStack(R.id.enhancedMainFragment, false)) {
                        navController.navigate(R.id.enhancedMainFragment)
                    }
                    true
                }
                else -> {
                    // Для Fragment навігації
                    Log.d(TAG, "Navigating to fragment: ${menuItem.itemId}")
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    val navigated = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    Log.d(TAG, "Navigation result: $navigated")
                    navigated
                }
            }
        }

        // Запускаємо оновлення серіалів у фоновому режимі
        startTvShowUpdates()
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
    /* Function removed */

    /**
     * Виправляє runtime для всіх серіалів
     * Викликається через подвійний довгий клік на тулбар
     */
    /* Function removed */

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