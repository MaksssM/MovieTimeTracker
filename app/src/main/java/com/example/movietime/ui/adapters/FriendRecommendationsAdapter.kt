package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.example.movietime.R
import com.example.movietime.data.firebase.FirebaseUser
import com.example.movietime.data.firebase.Recommendation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RecommendationWithUser(
    val recommendation: Recommendation,
    val fromUser: FirebaseUser
)

class FriendRecommendationsAdapter(
    private val onDetailsClick: (Recommendation) -> Unit,
    private val onMarkReadClick: (Recommendation) -> Unit
) : ListAdapter<RecommendationWithUser, FriendRecommendationsAdapter.RecommendationViewHolder>(RecommendationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val txtFromUser: TextView = itemView.findViewById(R.id.txtFromUser)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        private val txtMovieTitle: TextView = itemView.findViewById(R.id.txtMovieTitle)
        private val txtMovieYear: TextView = itemView.findViewById(R.id.txtMovieYear)
        private val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        private val btnDetails: MaterialButton = itemView.findViewById(R.id.btnDetails)
        private val btnMarkRead: MaterialButton = itemView.findViewById(R.id.btnMarkRead)

        fun bind(item: RecommendationWithUser) {
            val context = itemView.context
            val rec = item.recommendation
            val user = item.fromUser

            txtFromUser.text = user.displayName
            txtTime.text = getRelativeTime(rec.createdAt)

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
            txtMovieTitle.text = rec.contentTitle
            txtMovieYear.text = buildString {
                if (rec.mediaType == "movie") {
                    append(context.getString(R.string.movie))
                } else {
                    append(context.getString(R.string.tv_show))
                }
            }

            // Poster
            if (!rec.contentPoster.isNullOrEmpty()) {
                imgPoster.load("https://image.tmdb.org/t/p/w200${rec.contentPoster}") {
                    crossfade(true)
                }
            }

            // Message
            if (rec.message.isNotEmpty()) {
                txtMessage.isVisible = true
                txtMessage.text = "\"${rec.message}\""
            } else {
                txtMessage.isVisible = false
            }

            // Hide mark read if already read
            btnMarkRead.isVisible = !rec.isRead

            btnDetails.setOnClickListener { onDetailsClick(rec) }
            btnMarkRead.setOnClickListener { onMarkReadClick(rec) }
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

    private class RecommendationDiffCallback : DiffUtil.ItemCallback<RecommendationWithUser>() {
        override fun areItemsTheSame(oldItem: RecommendationWithUser, newItem: RecommendationWithUser): Boolean {
            return oldItem.recommendation.id == newItem.recommendation.id
        }

        override fun areContentsTheSame(oldItem: RecommendationWithUser, newItem: RecommendationWithUser): Boolean {
            return oldItem == newItem
        }
    }
}
