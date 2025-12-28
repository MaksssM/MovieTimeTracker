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

/**
 * Universal adapter for displaying content items (watched, planned, watching)
 * Optimized and reusable across different screens
 */
class ContentAdapter(
    private val onItemClick: (WatchedItem) -> Unit = {},
    private val onDeleteClick: (WatchedItem) -> Unit = {}
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
        return ContentViewHolder(binding, onItemClick, onDeleteClick)
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
        view.translationX = -60f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        
        val delay = (position * 50).toLong().coerceAtMost(300)
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationX", -60f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 350
            startDelay = delay
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    class ContentViewHolder(
        private val binding: ItemWatchedBinding,
        private val onItemClick: (WatchedItem) -> Unit,
        private val onDeleteClick: (WatchedItem) -> Unit
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
                    tvMediaType.text = "Фільм"
                    tvMediaType.setBackgroundResource(R.drawable.bg_badge_movie)
                }

                // Display rating if available
                item.voteAverage?.let { rating ->
                    if (rating > 0) {
                        // TODO: Add rating TextView to layout if needed
                        // For now, we'll append it to title or add later
                    }
                }

                // Load poster with Coil
                ivPoster.load(item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                ivPoster.contentDescription = root.context.getString(R.string.poster_description)

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
                        val intent = Intent(context, DetailsActivity::class.java).apply {
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

