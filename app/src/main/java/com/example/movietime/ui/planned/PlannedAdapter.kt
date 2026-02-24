package com.example.movietime.ui.planned

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemPlannedBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated adapter for Planned items with purple theme and unique layout.
 */
class PlannedAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {},
    private val onMoveToWatchingClick: (WatchedItem) -> Unit = {}
) : ListAdapter<WatchedItem, PlannedAdapter.PlannedViewHolder>(DiffCallback) {

    private var lastAnimatedPosition = -1

    fun updateItems(items: List<WatchedItem>) {
        lastAnimatedPosition = -1
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannedViewHolder {
        val binding = ItemPlannedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlannedViewHolder(binding, onItemClick, onDeleteClick, onMoveToWatchingClick)
    }

    override fun onBindViewHolder(holder: PlannedViewHolder, position: Int) {
        holder.bind(getItem(position))

        if (position > lastAnimatedPosition) {
            animateItem(holder.itemView, position)
            lastAnimatedPosition = position
        }
    }

    private fun animateItem(view: View, position: Int) {
        view.alpha = 0f
        view.translationX = -80f
        view.rotationY = 8f
        view.scaleX = 0.9f
        view.scaleY = 0.9f

        val delay = (position * 60).toLong().coerceAtMost(400)

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationX", -80f, 0f),
                ObjectAnimator.ofFloat(view, "rotationY", 8f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f)
            )
            duration = 500
            startDelay = delay
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    class PlannedViewHolder(
        private val binding: ItemPlannedBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit,
        private val onMoveToWatchingClick: (WatchedItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchedItem) {
            with(binding) {
                tvTitle.text = item.title
                tvReleaseDate.text = item.releaseDate?.take(4) ?: "N/A"
                tvRuntime.text = formatRuntime(item)

                // Media type badge
                if (item.mediaType == "tv") {
                    tvMediaType.text = "TV"
                } else {
                    tvMediaType.text = root.context.getString(R.string.movie_short)
                }

                // Rating
                val rating = item.voteAverage ?: 0.0
                if (rating > 0) {
                    layoutRating.visibility = View.VISIBLE
                    tvRating.text = String.format("%.1f", rating)
                } else {
                    layoutRating.visibility = View.GONE
                }

                // Load poster
                ivPoster.load(item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }) {
                    crossfade(200)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                // Added date
                item.lastUpdated?.let { timestamp ->
                    layoutAddedDate.isVisible = true
                    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                    tvAddedDate.text = dateFormat.format(Date(timestamp))
                } ?: run {
                    layoutAddedDate.isVisible = false
                }

                // Click — shared element transition
                root.setOnClickListener {
                    animateClick(it) {
                        val context = root.context
                        if (context is Activity) {
                            val targetActivity = if (item.mediaType == "tv") {
                                TvDetailsActivity::class.java
                            } else {
                                DetailsActivity::class.java
                            }
                            val intent = Intent(context, targetActivity).apply {
                                putExtra("ITEM_ID", item.id)
                                putExtra("MEDIA_TYPE", item.mediaType)
                            }
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                context, ivPoster, "poster_transition"
                            )
                            context.startActivity(intent, options.toBundle())
                        } else {
                            onItemClick(item)
                        }
                    }
                }

                btnDelete.setOnClickListener { view ->
                    animateBounce(view) { onDeleteClick(item) }
                }

                btnMoveToWatching.setOnClickListener { view ->
                    animateBounce(view) { onMoveToWatchingClick(item) }
                }
            }
        }

        private fun animateClick(view: View, action: () -> Unit) {
            view.animate()
                .scaleX(0.96f).scaleY(0.96f)
                .setDuration(80)
                .withEndAction {
                    view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                    action()
                }
                .start()
        }

        private fun animateBounce(view: View, action: () -> Unit) {
            view.animate()
                .scaleX(0.75f).scaleY(0.75f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator(3f))
                        .start()
                    action()
                }
                .start()
        }

        private fun formatRuntime(item: WatchedItem): String {
            if (item.mediaType == "tv") {
                val episodes = item.totalEpisodes ?: 0
                return if (episodes > 0) {
                    "$episodes ${binding.root.context.getString(R.string.episodes).lowercase()}"
                } else {
                    "TV"
                }
            }
            val minutes = item.runtime ?: 0
            if (minutes <= 0) return "N/A"
            val hours = minutes / 60
            val rem = minutes % 60
            return when {
                hours > 0 && rem > 0 -> "${hours}ч ${rem}мин"
                hours > 0 -> "${hours}ч"
                else -> "${minutes}мин"
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WatchedItem>() {
            override fun areItemsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType

            override fun areContentsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem == newItem
        }
    }
}
