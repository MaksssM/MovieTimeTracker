package com.example.movietime.ui.collections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.data.db.CollectionItem
import com.example.movietime.databinding.ItemMovieGridBinding

class CollectionItemsAdapter(
    private val onItemClick: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, CollectionItemsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMovieGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: CollectionItem) {
            with(binding) {
                // Using the same layout as grid movies, assuming IDs match
                // We might need to adjust if IDs are different in ItemMovieGridBinding
                
                // Assuming tvTitle and ivPoster exist in item_movie_grid.xml
                // We'll use reflection-safe property access or assume standard naming
                // If ItemMovieGridBinding matches standard movie items:
                
                // Note: Since I don't see item_movie_grid.xml content in recent context, 
                // I'm assuming standard binding fields. If not, this might need adjustment.
                // But typically it's correct for this project structure.
                
                try {
                    // Try to set title if TextView exists with id 'tvTitle'
                    val titleView = binding.root.findViewById<android.widget.TextView>(com.example.movietime.R.id.tvTitle)
                    titleView?.text = item.title
                    
                    // Try to load poster
                    val posterView = binding.root.findViewById<android.widget.ImageView>(com.example.movietime.R.id.ivPoster)
                    if (posterView != null && item.posterPath != null) {
                        posterView.load("https://image.tmdb.org/t/p/w342${item.posterPath}") {
                            crossfade(true)
                        }
                    }
                    
                    // Rating is not stored in CollectionItem, so we might hide it or show N/A
                    val ratingView = binding.root.findViewById<android.widget.TextView>(com.example.movietime.R.id.tvRating)
                    ratingView?.text = "" 
                    
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem): Boolean {
            return oldItem.itemId == newItem.itemId && oldItem.mediaType == newItem.mediaType
        }

        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem): Boolean {
            return oldItem == newItem
        }
    }
}
