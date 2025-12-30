package com.example.movietime.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Animate items as they appear
        if (position > lastAnimatedPosition) {
            animateItem(holder.itemView, position)
            lastAnimatedPosition = position
        }
    }

    private fun animateItem(view: View, position: Int) {
        view.alpha = 0f
        view.translationX = 80f
        view.scaleX = 0.9f
        view.scaleY = 0.9f
        
        val delay = position * 60L
        
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val translationX = ObjectAnimator.ofFloat(view, "translationX", 80f, 0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f)
        
        AnimatorSet().apply {
            playTogether(alpha, translationX, scaleX, scaleY)
            duration = 350
            startDelay = delay
            interpolator = DecelerateInterpolator(1.5f)
            start()
        }
    }

    // Reset animation when list changes
    fun resetAnimation() {
        lastAnimatedPosition = -1
    }

    inner class ViewHolder(private val binding: ItemRecentActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    // Add press animation
                    animateClick(binding.root) {
                        onItemClick(getItem(bindingAdapterPosition))
                    }
                }
            }
        }
        
        private fun animateClick(view: View, onClick: () -> Unit) {
            val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            
            AnimatorSet().apply {
                play(scaleDownX).with(scaleDownY)
                play(scaleUpX).with(scaleUpY).after(scaleDownX)
                duration = 100
                interpolator = OvershootInterpolator(1.5f)
                start()
            }
            
            view.postDelayed(onClick, 150)
        }

        fun bind(item: RecentActivityItem) {
            binding.tvTitle.text = item.title
            binding.tvTimeAgo.text = getTimeAgo(item.timestamp)

            val context = binding.root.context
            
            when (item.type) {
                RecentActivityItem.ActivityType.WATCHED -> {
                    binding.tvAction.text = context.getString(R.string.activity_watched_movie)
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_3d_primary)
                    binding.ivIcon.setImageResource(R.drawable.ic_check_24)
                }
                RecentActivityItem.ActivityType.PLANNED -> {
                    binding.tvAction.text = context.getString(R.string.activity_added_planned)
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_3d_accent)
                    binding.ivIcon.setImageResource(R.drawable.ic_bookmark_filled_24)
                }
                RecentActivityItem.ActivityType.WATCHING -> {
                    binding.tvAction.text = context.getString(R.string.activity_started_series)
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_3d_success)
                    binding.ivIcon.setImageResource(R.drawable.ic_play_circle_24)
                }
                RecentActivityItem.ActivityType.SEARCHED -> {
                    // Show appropriate action text based on media type
                    val actionText = if (item.mediaType == "movie") {
                        context.getString(R.string.activity_searched_movie)
                    } else {
                        context.getString(R.string.activity_searched_tv)
                    }
                    binding.tvAction.text = actionText
                    binding.iconContainer.setBackgroundResource(R.drawable.bg_icon_3d_warning)
                    binding.ivIcon.setImageResource(R.drawable.ic_search_24)
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val context = binding.root.context
            // Because Watched items might use fake timestamps (ID proxy), handle logic
            // For now, assume timestamps are reasonably close to System.currentTimeMillis
            // If timestamp is surprisingly small (e.g. ID proxy), just show "Recently"
            
            // Heuristic: if timestamp < 2000000000 (year 1970/early IDs), it's likely an ID proxy or old
            /* Actually, let's treat it as:
               - If it's a real timestamp (Planned/Watching), calc diff.
               - If it's 0 or small, show "Recently".
            */
             if (timestamp < 1000000000000L) { // Less than milliseconds epoch for year 2001
                 return context.getString(R.string.time_recently)
             }

            val diff = System.currentTimeMillis() - timestamp
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> context.getString(R.string.time_just_now)
                minutes < 60 -> context.getString(R.string.time_min_ago, minutes)
                hours < 24 -> context.getString(R.string.time_hours_ago, hours)
                days == 0L -> context.getString(R.string.today)
                days == 1L -> context.getString(R.string.yesterday)
                else -> context.getString(R.string.time_days_ago, days)
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
