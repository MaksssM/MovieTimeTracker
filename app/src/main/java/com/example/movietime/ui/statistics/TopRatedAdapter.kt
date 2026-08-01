package com.example.movietime.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.TopRatedItem
import com.example.movietime.databinding.ItemTopRatedStatBinding
import java.util.Locale

class TopRatedAdapter(
    private val onItemClick: (TopRatedItem) -> Unit
) : ListAdapter<TopRatedItem, TopRatedAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopRatedStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTopRatedStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopRatedItem) {
            binding.tvTitle.text = item.title
            binding.tvRating.text = String.format(Locale.US, "%.1f ⭐", item.userRating)
            binding.tvMediaType.text = if (item.mediaType == "movie") "🎬" else "📺"

            val posterUrl = if (!item.posterPath.isNullOrEmpty()) {
                "https://image.tmdb.org/t/p/w342${item.posterPath}"
            } else null

            binding.ivPoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TopRatedItem>() {
            override fun areItemsTheSame(oldItem: TopRatedItem, newItem: TopRatedItem) =
                oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType

            override fun areContentsTheSame(oldItem: TopRatedItem, newItem: TopRatedItem) =
                oldItem == newItem
        }
    }
}
