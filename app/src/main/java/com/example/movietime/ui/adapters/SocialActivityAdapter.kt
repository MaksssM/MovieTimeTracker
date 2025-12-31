package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.movietime.R
import com.example.movietime.data.firebase.ActivityType
import com.example.movietime.data.firebase.FirebaseUser
import com.example.movietime.data.firebase.SharedActivity
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SocialActivityItem(
    val activity: SharedActivity,
    val user: FirebaseUser,
    val isLikedByMe: Boolean = false
)

class SocialActivityAdapter(
    private val onLikeClick: (SharedActivity) -> Unit,
    private val onCommentClick: (SharedActivity) -> Unit,
    private val onRecommendClick: (SharedActivity) -> Unit,
    private val onActivityClick: (SharedActivity) -> Unit
) : ListAdapter<SocialActivityItem, SocialActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_social_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val txtUserName: TextView = itemView.findViewById(R.id.txtUserName)
        private val txtActivityType: TextView = itemView.findViewById(R.id.txtActivityType)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        private val txtMovieTitle: TextView = itemView.findViewById(R.id.txtMovieTitle)
        private val txtMovieYear: TextView = itemView.findViewById(R.id.txtMovieYear)
        private val layoutRating: LinearLayout = itemView.findViewById(R.id.layoutRating)
        private val txtRating: TextView = itemView.findViewById(R.id.txtRating)
        private val txtComment: TextView = itemView.findViewById(R.id.txtComment)
        private val btnLike: LinearLayout = itemView.findViewById(R.id.btnLike)
        private val imgLike: ImageView = itemView.findViewById(R.id.imgLike)
        private val txtLikes: TextView = itemView.findViewById(R.id.txtLikes)
        private val btnComment: LinearLayout = itemView.findViewById(R.id.btnComment)
        private val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        private val btnRecommend: ImageButton = itemView.findViewById(R.id.btnRecommend)

        fun bind(item: SocialActivityItem) {
            val context = itemView.context
            val activity = item.activity
            val user = item.user

            // User info
            txtUserName.text = user.displayName
            txtActivityType.text = getActivityTypeText(activity.activityType)
            txtTime.text = getRelativeTime(activity.createdAt)

            // User avatar
            if (!user.photoUrl.isNullOrEmpty()) {
                imgAvatar.load(user.photoUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person_24)
            }

            // Movie info
            txtMovieTitle.text = activity.contentTitle
            txtMovieYear.text = buildString {
                if (activity.mediaType == "movie") {
                    append(context.getString(R.string.movie))
                } else {
                    append(context.getString(R.string.tv_show))
                }
            }

            // Poster
            if (!activity.contentPoster.isNullOrEmpty()) {
                imgPoster.load("https://image.tmdb.org/t/p/w200${activity.contentPoster}") {
                    crossfade(true)
                }
            }

            // Rating
            val rating = activity.rating
            if (rating != null && rating > 0) {
                layoutRating.isVisible = true
                txtRating.text = String.format("%.1f", rating)
            } else {
                layoutRating.isVisible = false
            }

            // Review/Comment
            val review = activity.review
            if (!review.isNullOrEmpty()) {
                txtComment.isVisible = true
                txtComment.text = "\"$review\""
            } else {
                txtComment.isVisible = false
            }

            // Likes
            txtLikes.text = activity.likesCount.toString()
            if (item.isLikedByMe) {
                imgLike.setImageResource(R.drawable.ic_favorite_filled_24)
                imgLike.setColorFilter(0xFFEF4444.toInt())
            } else {
                imgLike.setImageResource(R.drawable.ic_favorite_border_24)
                imgLike.setColorFilter(0x80FFFFFF.toInt())
            }

            // Comments count
            txtComments.text = activity.commentsCount.toString()

            // Click listeners
            btnLike.setOnClickListener { onLikeClick(activity) }
            btnComment.setOnClickListener { onCommentClick(activity) }
            btnRecommend.setOnClickListener { onRecommendClick(activity) }
            itemView.setOnClickListener { onActivityClick(activity) }
        }

        private fun getActivityTypeText(type: ActivityType): String {
            return when (type) {
                ActivityType.WATCHED -> itemView.context.getString(R.string.activity_watched)
                ActivityType.WATCHING -> itemView.context.getString(R.string.activity_watching)
                ActivityType.PLANNED -> itemView.context.getString(R.string.activity_planned_to_watch)
                ActivityType.RATED -> itemView.context.getString(R.string.activity_rated)
                ActivityType.REVIEWED -> itemView.context.getString(R.string.activity_reviewed)
                ActivityType.RECOMMENDED -> itemView.context.getString(R.string.activity_recommended)
            }
        }

        private fun getRelativeTime(timestamp: Timestamp?): String {
            if (timestamp == null) return ""
            
            val now = System.currentTimeMillis()
            val time = timestamp.toDate().time
            val diff = now - time
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> itemView.context.getString(R.string.time_just_now)
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
                    itemView.context.getString(R.string.time_min_ago, mins)
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
                    itemView.context.getString(R.string.time_hours_ago, hours)
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                    itemView.context.getString(R.string.time_days_ago, days)
                }
                else -> {
                    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(time))
                }
            }
        }
    }

    private class ActivityDiffCallback : DiffUtil.ItemCallback<SocialActivityItem>() {
        override fun areItemsTheSame(oldItem: SocialActivityItem, newItem: SocialActivityItem): Boolean {
            return oldItem.activity.id == newItem.activity.id
        }

        override fun areContentsTheSame(oldItem: SocialActivityItem, newItem: SocialActivityItem): Boolean {
            return oldItem == newItem
        }
    }
}
