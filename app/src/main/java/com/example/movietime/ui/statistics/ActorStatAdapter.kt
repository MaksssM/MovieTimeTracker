package com.example.movietime.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.movietime.R
import com.example.movietime.data.model.ActorStatItem
import com.example.movietime.databinding.ItemActorStatBinding

class ActorStatAdapter(
    private val onItemClick: (ActorStatItem) -> Unit = {}
) : ListAdapter<ActorStatItem, ActorStatAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActorStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemActorStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: ActorStatItem, rank: Int) {
            val context = binding.root.context

            binding.tvRank.text = rank.toString()
            binding.tvActorName.text = item.actorName

            // Photo
            if (!item.profilePath.isNullOrBlank()) {
                binding.ivActorPhoto.load("https://image.tmdb.org/t/p/w185${item.profilePath}") {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_placeholder)
                    error(R.drawable.ic_person_placeholder)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivActorPhoto.load(R.drawable.ic_person_placeholder)
            }

            // Movies count and watch time
            val hours = item.totalWatchTimeMinutes / 60
            val watchTimeText = if (hours > 0) " • ${hours} ${context.getString(R.string.hours_short)}" else ""
            val countStr = context.resources.getQuantityString(
                R.plurals.movies_count, item.moviesWatched, item.moviesWatched
            )
            binding.tvMoviesCount.text = "$countStr$watchTimeText"

            // Movie titles preview
            if (item.movieTitles.isNotEmpty()) {
                val titlesPreview = item.movieTitles.take(3).joinToString(", ")
                binding.tvMovieTitles.text = if (item.movieTitles.size > 3) "$titlesPreview..." else titlesPreview
            } else {
                binding.tvMovieTitles.text = ""
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ActorStatItem>() {
            override fun areItemsTheSame(oldItem: ActorStatItem, newItem: ActorStatItem) =
                oldItem.actorId == newItem.actorId

            override fun areContentsTheSame(oldItem: ActorStatItem, newItem: ActorStatItem) =
                oldItem == newItem
        }
    }
}
