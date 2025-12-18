package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemWatchedBinding

/**
 * Universal adapter for displaying content items (watched, planned, watching)
 * Optimized and reusable across different screens
 */
class ContentAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {}
) : ListAdapter<WatchedItem, ContentAdapter.ContentViewHolder>(DiffCallback) {

    fun updateItems(items: List<WatchedItem>) {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val binding = ItemWatchedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentViewHolder(
        private val binding: ItemWatchedBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchedItem) {
            with(binding) {
                tvTitle.text = item.title
                tvReleaseDate.text = item.releaseDate?.take(4) ?: "N/A"
                tvRuntime.text = formatRuntime(item.runtime)

                // Display media type badge
                if (item.mediaType == "tv") {
                    tvMediaType.text = "TV"
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_tv)
                } else {
                    tvMediaType.text = "Фільм"
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_movie)
                }

                // Display rating if available
                item.voteAverage?.let { rating ->
                    if (rating > 0) {
                        // TODO: Add rating TextView to layout if needed
                        // For now, we'll append it to title or add later
                    }
                }

                // Load poster with Coil
                ivPoster.load(item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                ivPoster.contentDescription = root.context.getString(R.string.poster_description)

                // Click listeners
                root.setOnClickListener { onItemClick(item) }

                btnDelete.setOnClickListener { view ->
                    // Animate delete button
                    view.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(100)
                        .withEndAction {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            onDeleteClick(item)
                        }
                        .start()
                }
            }
        }

        private fun formatRuntime(runtime: Int?): String {
            val minutes = runtime ?: 0
            if (minutes <= 0) return "N/A"

            val hours = minutes / 60
            val remainingMinutes = minutes % 60

            return when {
                hours > 0 && remainingMinutes > 0 -> "$hours год $remainingMinutes хв"
                hours > 0 -> "$hours год"
                else -> "$minutes хв"
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

