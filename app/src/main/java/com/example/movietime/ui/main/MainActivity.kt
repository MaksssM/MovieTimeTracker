package com.example.movietime.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ActivityMainBinding
import com.example.movietime.ui.ViewModelFactory
import com.example.movietime.ui.search.SearchActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var watchedAdapter: WatchedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = ViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setupRecyclerView()
        setupItemTouchHelper()
        observeViewModel()

        binding.fabSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        watchedAdapter = WatchedAdapter()
        binding.rvWatchedList.apply {
            adapter = watchedAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(this@MainActivity, R.anim.layout_animation_fall_down)
        }
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = watchedAdapter.currentList[position]
                viewModel.deleteWatchedItem(item)

                Snackbar.make(binding.root, "Запись удалена", Snackbar.LENGTH_LONG).apply {
                    setAction("ОТМЕНИТЬ") {
                        viewModel.addWatchedItem(item)
                    }
                    show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvWatchedList)
    }


    private fun observeViewModel() {
        viewModel.allWatchedItems.observe(this) { items ->
            watchedAdapter.submitList(items)
            binding.tvEmptyState.isVisible = items.isEmpty()
        }

        viewModel.totalWatchTimeFormatted.observe(this) { formattedTime ->
            binding.tvTotalTime.text = formattedTime
        }
    }
}