package com.example.movietime.ui.collection

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.MovieResult
import com.example.movietime.databinding.ActivityCollectionDetailsBinding
import com.example.movietime.databinding.ItemCollectionMovieBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.movietime.data.repository.AppRepository

@AndroidEntryPoint
class CollectionDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionDetailsBinding
    
    @Inject
    lateinit var repository: AppRepository

    private val adapter = CollectionAdapter(
        onItemClick = { movie ->
            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtra("ITEM_ID", movie.id)
                putExtra("MEDIA_TYPE", "movie")
            }
            startActivity(intent)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        binding.rvMovies.layoutManager = LinearLayoutManager(this)
        binding.rvMovies.adapter = adapter

        val collectionId = intent.getIntExtra("COLLECTION_ID", -1)
        if (collectionId != -1) {
            loadCollectionDetails(collectionId)
        } else {
            finish()
        }
    }

    private fun loadCollectionDetails(collectionId: Int) {
        lifecycleScope.launch {
            try {
                // 1. Get collection details from API
                val details = repository.getCollectionDetails(collectionId) ?: return@launch

                // 2. Bind Basic Info
                binding.tvCollectionName.text = details.name
                binding.tvOverview.text = details.overview
                
                if (!details.backdropPath.isNullOrEmpty()) {
                    binding.ivBackdrop.load("https://image.tmdb.org/t/p/w1280${details.backdropPath}") {
                        crossfade(true)
                    }
                }

                // 3. Sort parts by release date
                val sortedParts = details.parts
                    .filter { !it.releaseDate.isNullOrEmpty() }
                    .sortedBy { it.releaseDate }

                // 4. Check watched status for each part
                val watchedIds = mutableSetOf<Int>()
                var watchedCount = 0
                
                // We need to check each item against the DB
                sortedParts.forEach { movie ->
                    val isWatched = repository.getWatchedItemById(movie.id, "movie") != null
                    if (isWatched) {
                        watchedIds.add(movie.id)
                        watchedCount++
                    }
                }

                // 5. Update Adapter
                adapter.submitList(sortedParts, watchedIds)

                // 6. Update Progress
                val total = sortedParts.size
                binding.progressCollection.max = total
                binding.progressCollection.progress = watchedCount
                binding.tvProgressText.text = getString(R.string.collection_progress, watchedCount, total)

                // 7. Determine "Next Up"
                val nextUp = sortedParts.firstOrNull { !watchedIds.contains(it.id) }
                if (nextUp != null) {
                    binding.cardNextUp.visibility = View.VISIBLE
                    binding.tvNextUpTitle.text = nextUp.title
                    binding.tvNextUpDate.text = nextUp.releaseDate?.take(4)
                    
                    if (!nextUp.posterPath.isNullOrEmpty()) {
                        binding.ivNextUpPoster.load("https://image.tmdb.org/t/p/w342${nextUp.posterPath}")
                    }
                    
                    binding.cardNextUp.setOnClickListener {
                        val intent = Intent(this@CollectionDetailsActivity, DetailsActivity::class.java).apply {
                            putExtra("ITEM_ID", nextUp.id)
                            putExtra("MEDIA_TYPE", "movie")
                        }
                        startActivity(intent)
                    }
                } else {
                    binding.cardNextUp.visibility = View.GONE
                }

                // 8. Dynamic Colors (Basic implementation)
                if (!details.posterPath.isNullOrEmpty()) {
                     // Could implement similar palette logic here if needed
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

class CollectionAdapter(
    private val onItemClick: (MovieResult) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    private var items: List<MovieResult> = emptyList()
    private var watchedIds: Set<Int> = emptySet()

    fun submitList(newItems: List<MovieResult>, newWatchedIds: Set<Int>) {
        items = newItems
        watchedIds = newWatchedIds
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemCollectionMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieResult) {
            binding.tvTitle.text = movie.title
            binding.tvYear.text = movie.releaseDate?.take(4) ?: ""
            binding.tvRating.text = String.format("%.1f", movie.voteAverage)
            
            if (!movie.posterPath.isNullOrEmpty()) {
                binding.ivPoster.load("https://image.tmdb.org/t/p/w342${movie.posterPath}") {
                    crossfade(true)
                    placeholder(R.color.poster_placeholder_dark)
                }
            } else {
                binding.ivPoster.setImageResource(R.color.poster_placeholder_dark)
            }

            val isWatched = watchedIds.contains(movie.id)
            binding.ivWatchedStatus.visibility = if (isWatched) View.VISIBLE else View.GONE
            
            // Dim watched items slightly
            binding.root.alpha = if (isWatched) 0.7f else 1.0f

            binding.root.setOnClickListener { onItemClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
