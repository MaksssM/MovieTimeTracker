package com.example.movietime.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.movietime.R
import com.example.movietime.data.model.DirectorStatItem
import com.example.movietime.databinding.ItemDirectorStatBinding

class DirectorStatAdapter(
    private val onItemClick: (DirectorStatItem) -> Unit = {}
) : ListAdapter<DirectorStatItem, DirectorStatAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDirectorStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemDirectorStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: DirectorStatItem, rank: Int) {
            val context = binding.root.context
            
            binding.tvRank.text = rank.toString()
            binding.tvDirectorName.text = item.directorName
            
            // Photo
            if (!item.profilePath.isNullOrBlank()) {
                binding.ivDirectorPhoto.load("https://image.tmdb.org/t/p/w185${item.profilePath}") {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_placeholder)
                    error(R.drawable.ic_person_placeholder)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivDirectorPhoto.load(R.drawable.ic_person_placeholder)
            }
            
            // Movies count and watch time
            val hours = item.totalWatchTimeMinutes / 60
            val watchTimeText = if (hours > 0) " • ${hours} ч" else ""
            binding.tvMoviesCount.text = context.resources.getQuantityString(
                R.plurals.movies_count, item.moviesWatched, item.moviesWatched
            ) + watchTimeText
            
            // Movie titles preview
            if (item.movieTitles.isNotEmpty()) {
                binding.tvMovieTitles.text = item.movieTitles.take(3).joinToString(", ") + 
                    if (item.movieTitles.size > 3) "..." else ""
            } else {
                binding.tvMovieTitles.text = ""
            }
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
