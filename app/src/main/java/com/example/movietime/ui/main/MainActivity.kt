package com.example.movietime.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.movietime.R
import com.example.movietime.databinding.DrawerlayoutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: DrawerlayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DrawerlayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштування Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Налаштування Toolbar для роботи з Navigation Drawer
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.watchedFragment, R.id.trendingFragment, R.id.settingsFragment
            ), binding.drawerLayout
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        // Сховання заголовка toolbar для всіх фрагментів
        navController.addOnDestinationChangedListener { _, _, _ ->
            binding.toolbar.title = null
        }

        // Підключення NavigationView до NavController
        binding.navView.setupWithNavController(navController)
    }
}