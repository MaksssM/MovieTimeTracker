package com.example.movietime.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ItemRecentActivityBinding
import com.example.movietime.data.model.RecentActivityItem
import java.util.concurrent.TimeUnit

class RecentActivityAdapter(
    private val onItemClick: (RecentActivityItem) -> Unit
) : ListAdapter<RecentActivityItem, RecentActivityAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecentActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(item: RecentActivityItem) {
            binding.tvTitle.text = item.title
            binding.tvTimeAgo.text = getTimeAgo(item.timestamp)

            val context = binding.root.context
            
            when (item.type) {
                RecentActivityItem.ActivityType.WATCHED -> {
                    binding.tvAction.text = context.getString(R.string.activity_watched_movie) // "Watched"
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_stat_icon_primary)
                    binding.ivIcon.setImageResource(R.drawable.ic_check_24)
                }
                RecentActivityItem.ActivityType.PLANNED -> {
                    binding.tvAction.text = context.getString(R.string.activity_added_planned) // "Added to Planned"
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_stat_icon_accent)
                    binding.ivIcon.setImageResource(R.drawable.ic_bookmark_filled_24)
                }
                RecentActivityItem.ActivityType.WATCHING -> {
                    binding.tvAction.text = context.getString(R.string.activity_started_series) // "Started watching"
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_stat_icon_success)
                    binding.ivIcon.setImageResource(R.drawable.ic_play_circle_24)
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            // Because Watched items might use fake timestamps (ID proxy), handle logic
            // For now, assume timestamps are reasonably close to System.currentTimeMillis
            // If timestamp is surprisingly small (e.g. ID proxy), just show "Recently"
            
            // Heuristic: if timestamp < 2000000000 (year 1970/early IDs), it's likely an ID proxy or old
            /* Actually, let's treat it as:
               - If it's a real timestamp (Planned/Watching), calc diff.
               - If it's 0 or small, show "Recently".
            */
             if (timestamp < 1000000000000L) { // Less than milliseconds epoch for year 2001
                 return "Recently"
             }

            val diff = System.currentTimeMillis() - timestamp
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hours ago"
                days == 0L -> "Today"
                days == 1L -> "Yesterday"
                else -> "$days days ago"
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RecentActivityItem>() {
            override fun areItemsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean {
                return oldItem.id == newItem.id && oldItem.type == newItem.type
            }

            override fun areContentsTheSame(oldItem: RecentActivityItem, newItem: RecentActivityItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
