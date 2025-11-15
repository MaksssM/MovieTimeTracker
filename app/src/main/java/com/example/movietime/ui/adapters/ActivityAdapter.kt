package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.databinding.ItemActivityBinding
import com.example.movietime.data.model.Activity
import com.example.movietime.data.model.ActivityType
import com.example.movietime.util.DateTimeUtils

class ActivityAdapter(
    private val onItemClick: (Activity) -> Unit
) : ListAdapter<Activity, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(
        private val binding: ItemActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(activity: Activity) {
            with(binding) {
                // Set user info
                tvUsername.text = activity.username
                tvTimeAgo.text = formatTimeAgo(activity.createdDate)

                // Set activity type
                tvActivityType.text = getActivityTypeText(activity.type)

                // Set movie/TV show title
                tvMovieTitle.text = activity.movieTitle ?: ""

                // Load poster image
                activity.moviePoster?.let { posterPath ->
                    ivMoviePoster.load("https://image.tmdb.org/t/p/w185$posterPath") {
                        crossfade(true)
                        placeholder(R.drawable.rounded_corner_background)
                        error(R.drawable.rounded_corner_background)
                    }
                }

                // Show rating if available
                activity.rating?.let { rating ->
                    layoutRating.isVisible = true
                    tvRating.text = String.format("%.1f", rating)
                } ?: run {
                    layoutRating.isVisible = false
                }

                // Show review if available
                activity.review?.let { review ->
                    tvReview.isVisible = true
                    tvReview.text = review
                } ?: run {
                    tvReview.isVisible = false
                }

                // Set click listener
                root.setOnClickListener {
                    onItemClick(activity)
                }
            }
        }

        private fun getActivityTypeText(type: ActivityType): String {
            return when (type) {
                ActivityType.WATCHED_MOVIE -> binding.root.context.getString(R.string.activity_watched_movie)
                ActivityType.WATCHED_TV_SHOW -> binding.root.context.getString(R.string.activity_watched_tv)
                ActivityType.RATED_MOVIE -> binding.root.context.getString(R.string.activity_rated_movie)
                ActivityType.RATED_TV_SHOW -> binding.root.context.getString(R.string.activity_rated_tv)
                ActivityType.ADDED_TO_PLANNED -> binding.root.context.getString(R.string.activity_added_to_planned)
                ActivityType.WROTE_REVIEW -> binding.root.context.getString(R.string.activity_wrote_review)
            }
        }

        private fun formatTimeAgo(dateTime: String): String {
            return DateTimeUtils.formatTimeAgo(dateTime)
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<Activity>() {
        override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem == newItem
        }
    }
}
