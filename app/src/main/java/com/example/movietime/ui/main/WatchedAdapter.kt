package com.example.movietime.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Size
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemWatchedBinding

class WatchedAdapter : ListAdapter<WatchedItem, WatchedAdapter.WatchedViewHolder>(DiffCallback) {

    var onItemClick: ((WatchedItem) -> Unit)? = null
    var onDeleteClick: ((WatchedItem) -> Unit)? = null
    var onRewatchClick: ((WatchedItem) -> Unit)? = null

    fun updateItems(items: List<WatchedItem>) {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedViewHolder {
        val binding = ItemWatchedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WatchedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchedViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class WatchedViewHolder(private val binding: ItemWatchedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WatchedItem) {
            binding.tvTitle.text = item.title

            // Release date
            binding.tvReleaseDate.text = item.releaseDate?.take(4) ?: "N/A"

            // Runtime
            val runtime = item.runtime ?: 0
            binding.tvRuntime.text = if (runtime > 0) {
                val hours = runtime / 60
                val minutes = runtime % 60
                when {
                    hours > 0 && minutes > 0 -> "$hours год $minutes хв"
                    hours > 0 -> "$hours год"
                    else -> "$minutes хв"
                }
            } else {
                "N/A"
            }

            // Rewatch count badge
            if (item.watchCount > 1) {
                binding.layoutRewatchCount.visibility = View.VISIBLE
                binding.tvRewatchCount.text = binding.root.context.getString(
                    com.example.movietime.R.string.watch_count, 
                    item.watchCount
                )
            } else {
                binding.layoutRewatchCount.visibility = View.GONE
            }

            // Poster with Coil
            binding.ivPoster.load(item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }) {
                crossfade(true)
                size(500, 750)
                placeholder(com.example.movietime.R.drawable.ic_placeholder)
                error(com.example.movietime.R.drawable.ic_placeholder)
            }

            binding.ivPoster.contentDescription = binding.root.context.getString(com.example.movietime.R.string.poster_description)

            // Item click
            binding.root.setOnClickListener { onItemClick?.invoke(item) }

            // Rewatch button click with animation
            binding.btnAddRewatch.setOnClickListener {
                it.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(100)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        onRewatchClick?.invoke(item)
                    }
                    .start()
            }

            // Delete click with animation
            binding.btnDelete.setOnClickListener {
                it.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(100)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        onDeleteClick?.invoke(item)
                    }
                    .start()
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WatchedItem>() {
            override fun areItemsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType

            override fun areContentsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem == newItem
        }
    }
}