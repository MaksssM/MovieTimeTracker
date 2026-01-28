package com.example.movietime.ui.today.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.movietime.R
import com.example.movietime.data.model.ContinueWatchingItem
import com.example.movietime.databinding.ItemContinueWatchingBinding

class ContinueWatchingAdapter(
    private val onItemClick: (ContinueWatchingItem) -> Unit
) : ListAdapter<ContinueWatchingItem, ContinueWatchingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContinueWatchingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemContinueWatchingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: ContinueWatchingItem) {
            binding.tvTitle.text = item.title
            
            // Episode info
            val episodeText = if (item.mediaType == "tv") {
                item.nextEpisode ?: item.lastWatchedEpisode ?: ""
            } else {
                ""
            }
            binding.tvEpisode.text = episodeText

            // Progress
            val progressPercent = (item.progress * 100).toInt()
            binding.progressBar.progress = progressPercent
            binding.tvProgress.text = binding.root.context.getString(
                R.string.percent_watched, progressPercent
            )

            // Load poster/backdrop
            val imageUrl = item.backdropPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                ?: item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
            
            binding.ivPoster.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_poster)
                error(R.drawable.placeholder_poster)
                transformations(RoundedCornersTransformation(16f))
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ContinueWatchingItem>() {
        override fun areItemsTheSame(oldItem: ContinueWatchingItem, newItem: ContinueWatchingItem): Boolean {
            return oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType
        }

        override fun areContentsTheSame(oldItem: ContinueWatchingItem, newItem: ContinueWatchingItem): Boolean {
            return oldItem == newItem
        }
    }
}
