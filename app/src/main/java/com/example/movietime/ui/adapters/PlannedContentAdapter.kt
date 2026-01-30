package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemPlannedBinding

/**
 * Adapter for displaying planned content items
 */
class PlannedContentAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {}
) : ListAdapter<WatchedItem, PlannedContentAdapter.PlannedViewHolder>(DiffCallback) {

    fun updateItems(items: List<WatchedItem>) {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannedViewHolder {
        val binding = ItemPlannedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlannedViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: PlannedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlannedViewHolder(
        private val binding: ItemPlannedBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchedItem) {
            with(binding) {
                tvTitle.text = item.title
                tvReleaseDate.text = item.releaseDate?.take(4) ?: "N/A"

                // Display media type badge
                if (item.mediaType == "tv") {
                    tvMediaType.text = "TV"
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_tv)
                    // For TV shows, display number of episodes
                    tvEpisodes.text = "${item.totalEpisodes ?: 0}"
                    tvSeasons.text = binding.root.context.getString(R.string.episodes_label)
                } else {
                    tvMediaType.text = binding.root.context.getString(R.string.movie_short)
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_movie)
                    // For movies, display duration
                    tvEpisodes.text = formatRuntime(item.runtime)
                    tvSeasons.text = binding.root.context.getString(R.string.movie_label)
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
