package com.example.movietime.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.data.model.GenreStatItem
import com.example.movietime.databinding.ItemGenreStatBinding

class GenreStatAdapter : ListAdapter<GenreStatItem, GenreStatAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGenreStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemGenreStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GenreStatItem, rank: Int) {
            val context = binding.root.context
            
            binding.tvRank.text = rank.toString()
            binding.tvGenreName.text = item.genreName
            binding.tvCount.text = context.resources.getQuantityString(
                R.plurals.movies_count, item.count, item.count
            )
            
            // Progress bar (percentage)
            binding.progressGenre.progress = item.percentage.toInt().coerceIn(0, 100)
            
            // Watch time
            val hours = item.totalWatchTimeMinutes / 60
            val watchTimeText = if (hours > 0) {
                "⏱️ ${hours} ч"
            } else {
                "⏱️ ${item.totalWatchTimeMinutes} мин"
            }
            binding.tvWatchTime.text = watchTimeText
            
            // Average rating
            if (item.averageRating > 0) {
                binding.tvAvgRating.text = "⭐ %.1f".format(item.averageRating)
            } else {
                binding.tvAvgRating.text = ""
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GenreStatItem>() {
            override fun areItemsTheSame(oldItem: GenreStatItem, newItem: GenreStatItem) =
                oldItem.genreId == newItem.genreId

            override fun areContentsTheSame(oldItem: GenreStatItem, newItem: GenreStatItem) =
                oldItem == newItem
        }
    }
}
