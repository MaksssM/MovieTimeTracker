package com.example.movietime.ui.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemWatchedBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity

/**
 * Universal adapter for displaying content items (watched, planned, watching)
 * Optimized and reusable across different screens
 */
class ContentAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {},
    private val onRewatchClick: (WatchedItem) -> Unit = {}
) : ListAdapter<WatchedItem, ContentAdapter.ContentViewHolder>(DiffCallback) {

    private var lastAnimatedPosition = -1

    fun updateItems(items: List<WatchedItem>) {
        lastAnimatedPosition = -1
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val binding = ItemWatchedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding, onItemClick, onDeleteClick, onRewatchClick)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Animate items only on first appearance
        if (position > lastAnimatedPosition) {
            animateItem(holder.itemView, position)
            lastAnimatedPosition = position
        }
    }
    
    private fun animateItem(view: android.view.View, position: Int) {
        view.alpha = 0f
        view.translationY = 60f
        view.scaleX = 0.92f
        view.scaleY = 0.92f
        
        val delay = (position * 50).toLong().coerceAtMost(350)
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 60f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.92f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.92f, 1f)
            )
            duration = 400
            startDelay = delay
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }

    class ContentViewHolder(
        private val binding: ItemWatchedBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit,
        private val onRewatchClick: (WatchedItem) -> Unit = {}
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchedItem) {
            with(binding) {
                tvTitle.text = item.title
                tvReleaseDate.text = item.releaseDate?.take(4) ?: "N/A"
                tvRuntime.text = formatRuntime(item.runtime)

                // Display media type badge
                if (item.mediaType == "tv") {
                    tvMediaType.text = "TV"
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_tv)
                } else {
                    tvMediaType.text = binding.root.context.getString(R.string.movie_short)
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_movie)
                }

                // Accent bar colour by media type (blue = movie, amber = TV)
                viewAccentBar.setBackgroundResource(
                    if (item.mediaType == "tv") R.drawable.bg_accent_bar_tv
                    else R.drawable.bg_accent_bar_movie
                )

                // Display rating badge
                val rating = item.voteAverage ?: 0.0
                if (rating > 0) {
                    binding.layoutRating?.visibility = android.view.View.VISIBLE
                    binding.tvRating?.text = String.format("%.1f", rating)
                } else {
                    binding.layoutRating?.visibility = android.view.View.GONE
                }

                // Load poster with Coil
                ivPoster.load(item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                ivPoster.contentDescription = root.context.getString(R.string.poster_description)

                // Display rewatch count if > 1
                layoutRewatchCount?.visibility = if (item.watchCount > 1) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                if (item.watchCount > 1) {
                    tvRewatchCount?.text = if (item.mediaType == "tv") {
                        "×${item.watchCount} разів"
                    } else {
                        "×${item.watchCount}"
                    }
                }

                // Click listeners з shared element transition
                root.setOnClickListener {
                    // Add press animation
                    it.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(80)
                        .withEndAction {
                            it.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .setInterpolator(OvershootInterpolator(2f))
                                .start()
                        }
                        .start()
                    
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
                            context,
                            ivPoster,
                            "poster_transition"
                        )
                        context.startActivity(intent, options.toBundle())
                    } else {
                        onItemClick(item)
                    }
                }

                btnDelete.setOnClickListener { view ->
                    // Animate delete button
                    view.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(100)
                        .withEndAction {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            onDeleteClick(item)
                        }
                        .start()
                }

                // Add rewatch button click listener if view exists
                btnAddRewatch?.setOnClickListener { view ->
                    // Animate rewatch button with bounce effect
                    view.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(100)
                        .withEndAction {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .setInterpolator(OvershootInterpolator(2f))
                                .start()
                            onRewatchClick(item)
                        }
                        .start()
                }
            }
        }

        private fun formatRuntime(runtime: Int?): String {
            val minutes = runtime ?: 0
            if (minutes <= 0) return "N/A"

            val hours = minutes / 60
            val remainingMinutes = minutes % 60

            return when {
                hours > 0 && remainingMinutes > 0 -> "$hours год $remainingMinutes хв"
                hours > 0 -> "$hours год"
                else -> "$minutes хв"
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

