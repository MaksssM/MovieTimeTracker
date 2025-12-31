package com.example.movietime.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ItemEpisodeProgressBinding
import com.example.movietime.util.Utils

/**
 * Дані епізоду для відображення в UI
 */
data class EpisodeUiModel(
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val runtime: Int?, // в хвилинах
    var isWatched: Boolean = false
)

class EpisodeProgressAdapter(
    private val onEpisodeChecked: (EpisodeUiModel, Boolean) -> Unit
) : ListAdapter<EpisodeUiModel, EpisodeProgressAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEpisodeProgressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEpisodeProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: EpisodeUiModel) {
            binding.apply {
                tvEpisodeNumber.text = episode.episodeNumber.toString()
                tvEpisodeName.text = episode.name.ifEmpty { 
                    root.context.getString(R.string.episode_format, episode.episodeNumber) 
                }
                
                tvEpisodeRuntime.text = if (episode.runtime != null && episode.runtime > 0) {
                    "${episode.runtime} хв"
                } else {
                    "—"
                }
                
                // Checkbox state
                cbEpisode.setOnCheckedChangeListener(null)
                cbEpisode.isChecked = episode.isWatched
                
                // Watched indicator
                ivWatched.visibility = if (episode.isWatched) View.VISIBLE else View.GONE
                
                // Text color based on watched state
                val textColor = if (episode.isWatched) {
                    root.context.getColor(R.color.success)
                } else {
                    root.context.getColor(android.R.color.white)
                }
                tvEpisodeName.setTextColor(textColor)
                
                // Click listeners
                cbEpisode.setOnCheckedChangeListener { _, isChecked ->
                    episode.isWatched = isChecked
                    onEpisodeChecked(episode, isChecked)
                    // Update UI immediately
                    ivWatched.visibility = if (isChecked) View.VISIBLE else View.GONE
                    tvEpisodeName.setTextColor(
                        if (isChecked) root.context.getColor(R.color.success) 
                        else root.context.getColor(android.R.color.white)
                    )
                }
                
                root.setOnClickListener {
                    cbEpisode.isChecked = !cbEpisode.isChecked
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EpisodeUiModel>() {
        override fun areItemsTheSame(oldItem: EpisodeUiModel, newItem: EpisodeUiModel): Boolean {
            return oldItem.seasonNumber == newItem.seasonNumber && 
                   oldItem.episodeNumber == newItem.episodeNumber
        }

        override fun areContentsTheSame(oldItem: EpisodeUiModel, newItem: EpisodeUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
