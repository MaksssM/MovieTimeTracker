package com.example.movietime.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.DirectorStatItem
import com.example.movietime.databinding.ItemCastBinding

class DirectorStatAdapter(
    private val onItemClick: (DirectorStatItem) -> Unit = {}
) : ListAdapter<DirectorStatItem, DirectorStatAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: DirectorStatItem) {
            val context = binding.root.context

            binding.tvName.text = item.directorName

            // Photo
            if (!item.profilePath.isNullOrBlank()) {
                binding.ivProfile.load("https://image.tmdb.org/t/p/w185${item.profilePath}") {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_placeholder)
                    error(R.drawable.ic_person_placeholder)
                }
            } else {
                binding.ivProfile.setImageResource(R.drawable.ic_person_placeholder)
            }

            // Titles count and watch time
            val hours = item.totalWatchTimeMinutes / 60
            val watchTimeText = if (hours > 0) " • ${hours} ч" else ""
            binding.tvCharacter.text = context.resources.getQuantityString(
                R.plurals.movies_count, item.moviesWatched, item.moviesWatched
            ) + watchTimeText
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DirectorStatItem>() {
            override fun areItemsTheSame(oldItem: DirectorStatItem, newItem: DirectorStatItem) =
                oldItem.directorId == newItem.directorId

            override fun areContentsTheSame(oldItem: DirectorStatItem, newItem: DirectorStatItem) =
                oldItem == newItem
        }
    }
}
