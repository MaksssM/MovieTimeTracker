package com.example.movietime.ui.universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.movietime.ui.base.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.ActivityUniversesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UniversesActivity : BaseActivity() {

    private lateinit var binding: ActivityUniversesBinding
    private val viewModel: UniversesViewModel by viewModels()

    private val adapter = UniversesAdapter { universe ->
        val intent = Intent(this, UniverseDetailActivity::class.java).apply {
            putExtra(UniverseDetailActivity.EXTRA_UNIVERSE_ID, universe.universe.id)
            putExtra(UniverseDetailActivity.EXTRA_UNIVERSE_NAME, universe.universe.name)
        }
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUniversesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.rvUniverses.layoutManager = LinearLayoutManager(this)
        binding.rvUniverses.adapter = adapter

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.universes.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, UniversesActivity::class.java))
        }
    }
}
