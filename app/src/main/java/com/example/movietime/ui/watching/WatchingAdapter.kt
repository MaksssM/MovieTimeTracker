package com.example.movietime.ui.watching

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemWatchingBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity

/**
 * Dedicated adapter for Watching items with green theme, progress bars, and unique animations.
 */
class WatchingAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {},
    private val onMoveToWatchedClick: (WatchedItem) -> Unit = {}
) : ListAdapter<WatchedItem, WatchingAdapter.WatchingViewHolder>(DiffCallback) {

    private var lastAnimatedPosition = -1

    fun updateItems(items: List<WatchedItem>) {
        lastAnimatedPosition = -1
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchingViewHolder {
        val binding = ItemWatchingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WatchingViewHolder(binding, onItemClick, onDeleteClick, onMoveToWatchedClick)
    }

    override fun onBindViewHolder(holder: WatchingViewHolder, position: Int) {
        holder.bind(getItem(position))

        if (position > lastAnimatedPosition) {
            animateItem(holder, position)
            lastAnimatedPosition = position
        }
    }

    private fun animateItem(holder: WatchingViewHolder, position: Int) {
        val view = holder.itemView
        view.alpha = 0f
        view.translationY = 80f
        view.scaleX = 0.88f
        view.scaleY = 0.88f

        val delay = (position * 55).toLong().coerceAtMost(380)

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 80f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.88f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.88f, 1f)
            )
            duration = 450
            startDelay = delay
            interpolator = OvershootInterpolator(1.4f)
            start()
        }

        // Animate the active indicator with pulsing
        holder.animateActiveIndicator()
    }

    class WatchingViewHolder(
        private val binding: ItemWatchingBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit,
        private val onMoveToWatchedClick: (WatchedItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var pulseAnimator: ObjectAnimator? = null

        fun animateActiveIndicator() {
            pulseAnimator?.cancel()
            pulseAnimator = ObjectAnimator.ofFloat(binding.viewActiveIndicator, "alpha", 1f, 0.3f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }

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

                // Progress for TV shows
                if (item.mediaType == "tv" && item.totalEpisodes != null && item.totalEpisodes > 0) {
                    layoutProgress.isVisible = true
                    val watched = item.watchCount.coerceAtMost(item.totalEpisodes)
                    val total = item.totalEpisodes
                    val percent = ((watched.toFloat() / total) * 100).toInt().coerceIn(0, 100)

                    tvProgress.text = root.context.getString(R.string.episodes_progress, watched, total)
                    tvProgressPercent.text = "$percent%"

                    // Animate progress bar
                    val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, percent)
                    animator.duration = 800
                    animator.interpolator = AccelerateDecelerateInterpolator()
                    animator.startDelay = 200
                    animator.start()
                } else {
                    layoutProgress.isVisible = false
                }

                // Click with shared element
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

                btnMoveToWatched.setOnClickListener { view ->
                    // Special animation: scale + checkmark glow
                    view.animate()
                        .scaleX(0.7f).scaleY(0.7f)
                        .rotation(15f)
                        .setDuration(120)
                        .withEndAction {
                            view.animate()
                                .scaleX(1.1f).scaleY(1.1f)
                                .rotation(0f)
                                .setDuration(200)
                                .setInterpolator(OvershootInterpolator(4f))
                                .withEndAction {
                                    view.animate()
                                        .scaleX(1f).scaleY(1f)
                                        .setDuration(100)
                                        .start()
                                    onMoveToWatchedClick(item)
                                }
                                .start()
                        }
                        .start()
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
