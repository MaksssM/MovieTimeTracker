package com.example.movietime.ui.today.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.movietime.R
import com.example.movietime.data.model.NewEpisodeItem
import com.example.movietime.databinding.ItemNewEpisodeBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class NewEpisodeAdapter(
    private val onItemClick: (NewEpisodeItem) -> Unit
) : ListAdapter<NewEpisodeItem, NewEpisodeAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNewEpisodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNewEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.btnAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: NewEpisodeItem) {
            val context = binding.root.context
            
            binding.tvShowTitle.text = item.tvShowName
            
            // Episode info: "S1 E5 • Episode Name"
            val episodeCode = "S${item.seasonNumber} E${item.episodeNumber}"
            val episodeInfo = if (!item.episodeName.isNullOrBlank()) {
                "$episodeCode • ${item.episodeName}"
            } else {
                episodeCode
            }
            binding.tvEpisodeInfo.text = episodeInfo

            // Air date
            binding.tvAirDate.text = formatAirDate(item.airDate, context)

            // Watched state
            binding.ivWatched.isVisible = item.isWatched
            binding.tvNewBadge.isVisible = !item.isWatched
            
            binding.btnAction.setImageResource(
                if (item.isWatched) R.drawable.ic_check_24 else R.drawable.ic_play_24
            )

            // Load still image (backdrop) or poster
            val imageUrl = item.backdropPath?.let { "https://image.tmdb.org/t/p/w300$it" }
                ?: item.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" }
            
            binding.ivPoster.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_episode)
                error(R.drawable.placeholder_episode)
                transformations(RoundedCornersTransformation(12f))
            }
        }

        private fun formatAirDate(dateStr: String?, context: android.content.Context): String {
            if (dateStr.isNullOrBlank()) return ""
            
            return try {
                val airDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                val today = LocalDate.now()
                val daysDiff = ChronoUnit.DAYS.between(airDate, today)
                
                when {
                    daysDiff == 0L -> context.getString(R.string.aired_today)
                    daysDiff == 1L -> context.getString(R.string.aired_yesterday)
                    daysDiff in 2..7 -> context.getString(R.string.aired_days_ago, daysDiff.toInt())
                    else -> {
                        val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                        formatter.format(java.sql.Date.valueOf(dateStr))
                    }
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<NewEpisodeItem>() {
        override fun areItemsTheSame(oldItem: NewEpisodeItem, newItem: NewEpisodeItem): Boolean {
            return oldItem.tvShowId == newItem.tvShowId && 
                   oldItem.seasonNumber == newItem.seasonNumber &&
                   oldItem.episodeNumber == newItem.episodeNumber
        }

        override fun areContentsTheSame(oldItem: NewEpisodeItem, newItem: NewEpisodeItem): Boolean {
            return oldItem == newItem
        }
    }
}
