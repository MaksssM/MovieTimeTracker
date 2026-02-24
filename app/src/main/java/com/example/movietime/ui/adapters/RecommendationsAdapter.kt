package com.example.movietime.ui.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.databinding.ItemContentPosterBinding

class RecommendationsAdapter(
    private val onItemClick: (Int, String) -> Unit // id, mediaType ("movie" or "tv")
) : ListAdapter<Any, RecommendationsAdapter.ViewHolder>(DiffCallback) {

    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentPosterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Animate horizontal items with stagger effect
        if (position > lastAnimatedPosition) {
            animateItem(holder.itemView, position)
            lastAnimatedPosition = position
        }
    }

    private fun animateItem(view: View, position: Int) {
        view.alpha = 0f
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.translationY = 30f
        
        val delay = position * 50L
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.85f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.85f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 30f, 0f)
            )
            duration = 400
            startDelay = delay
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    fun resetAnimation() {
        lastAnimatedPosition = -1
    }

    class ViewHolder(
        private val binding: ItemContentPosterBinding,
        private val onItemClick: (Int, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any) {
            var title: String? = null
            var posterPath: String? = null
            var voteAverage: Double? = null
            var id = 0
            var mediaType = "movie"

            when (item) {
                is MovieResult -> {
                    title = item.title
                    posterPath = item.posterPath
                    voteAverage = item.voteAverage.toDouble()
                    id = item.id
                    mediaType = "movie"
                }
                is TvShowResult -> {
                    title = item.name
                    posterPath = item.posterPath
                    voteAverage = item.voteAverage.toDouble()
                    id = item.id
                    mediaType = "tv"
                }
            }

            with(binding) {
                tvTitle.text = title ?: "Unknown"
                
                // Rating
                if (voteAverage != null && voteAverage > 0) {
                    tvRating.text = String.format("⭐ %.1f", voteAverage)
                    tvRating.visibility = android.view.View.VISIBLE
                } else {
                    tvRating.visibility = android.view.View.GONE
                }

                // Poster
                ivPoster.load(posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }) {
                    crossfade(true)
                    crossfade(200)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }

                // Click animation
                root.setOnClickListener { view ->
                    animateClick(view) { onItemClick(id, mediaType) }
                }
            }
        }
        
        private fun animateClick(view: View, action: () -> Unit) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.92f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f, 1f)
                )
                duration = 200
                interpolator = DecelerateInterpolator()
                start()
            }
            view.postDelayed(action, 150)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> oldItem.id == newItem.id
                    oldItem is TvShowResult && newItem is TvShowResult -> oldItem.id == newItem.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> oldItem == newItem
                    oldItem is TvShowResult && newItem is TvShowResult -> oldItem == newItem
                    else -> false
                }
            }
        }
    }
}
