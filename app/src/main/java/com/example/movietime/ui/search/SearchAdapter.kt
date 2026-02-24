package com.example.movietime.ui.search

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
import coil.size.Size
import com.example.movietime.R
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.Person
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.databinding.ItemSearchResultBinding
import com.example.movietime.databinding.ItemPersonResultBinding
import java.util.Locale

class SearchAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_MEDIA = 0
        private const val VIEW_TYPE_PERSON = 1
        const val PAYLOAD_HIGHLIGHT = "highlight"
        
        private val DiffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> oldItem.id == newItem.id
                    oldItem is TvShowResult && newItem is TvShowResult -> oldItem.id == newItem.id
                    oldItem is Person && newItem is Person -> oldItem.id == newItem.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> 
                        oldItem.id == newItem.id && oldItem.title == newItem.title && 
                        oldItem.posterPath == newItem.posterPath && oldItem.voteAverage == newItem.voteAverage
                    oldItem is TvShowResult && newItem is TvShowResult -> 
                        oldItem.id == newItem.id && oldItem.name == newItem.name && 
                        oldItem.posterPath == newItem.posterPath && oldItem.voteAverage == newItem.voteAverage
                    oldItem is Person && newItem is Person -> 
                        oldItem.id == newItem.id && oldItem.name == newItem.name && 
                        oldItem.profilePath == newItem.profilePath
                    else -> false
                }
            }
        }
    }

    var onItemClick: ((Any) -> Unit)? = null
    var onItemLongClick: ((Any) -> Unit)? = null
    var currentQuery: String = ""
    private var lastAnimatedPosition = -1

    fun updateQueryHighlight(q: String) {
        val old = currentQuery
        currentQuery = q.trim()
        if (old != currentQuery) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_HIGHLIGHT)
        }
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < itemCount) {
            val currentList = currentList.toMutableList()
            currentList.removeAt(position)
            submitList(currentList)
        }
    }
    
    override fun submitList(list: List<Any>?) {
        lastAnimatedPosition = -1
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Person -> VIEW_TYPE_PERSON
            else -> VIEW_TYPE_MEDIA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PERSON -> {
                val binding = ItemPersonResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PersonViewHolder(binding)
            }
            else -> {
                val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MediaViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        
        when (holder) {
            is MediaViewHolder -> holder.bind(currentItem)
            is PersonViewHolder -> holder.bind(currentItem as Person)
        }
        
        val adapterPosition = holder.bindingAdapterPosition
        if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition > lastAnimatedPosition) {
            animateItemEntry(holder.itemView, adapterPosition)
            lastAnimatedPosition = adapterPosition
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_HIGHLIGHT)) {
            // Only update text highlighting, skip full rebind and image reload
            when (holder) {
                is MediaViewHolder -> holder.updateHighlight()
                // PersonViewHolder doesn't use highlighting
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
    }
    
    private fun animateItemEntry(view: View, position: Int) {
        view.alpha = 0f
        view.translationY = 40f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        
        val delay = (position * 40).toLong().coerceAtMost(250)
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 40f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 300
            startDelay = delay
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    // ViewHolder for Movies and TV Shows
    inner class MediaViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { view ->
                view.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .setInterpolator(OvershootInterpolator(2f))
                            .start()
                    }
                    .start()
                    
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }

            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position))
                    animateRemoval(binding.root)
                    removeItem(position)
                }
                true
            }
        }

        private var lastBoundTitle: String = ""
        private var lastBoundOverview: String = ""

        fun bind(item: Any) {
            when (item) {
                is MovieResult -> {
                    lastBoundTitle = item.title ?: binding.root.context.getString(R.string.no_title)
                    binding.tvTitle.text = lastBoundTitle

                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(200)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }

                    val rating = item.voteAverage
                    binding.tvRating.text = if (rating > 0f) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }

                    // Media type with country
                    val country = item.productionCountries?.firstOrNull()?.iso?.let { getCountryFlag(it) } ?: ""
                    binding.tvMediaType.text = "$country ${binding.root.context.getString(R.string.media_type_movie)}"
                    binding.viewAccentBar.setBackgroundResource(R.drawable.bg_accent_bar_movie)

                    lastBoundOverview = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)
                    binding.tvOverview.text = lastBoundOverview

                    highlightText(binding.tvTitle, lastBoundTitle)
                    highlightText(binding.tvOverview, lastBoundOverview)
                }
                is TvShowResult -> {
                    lastBoundTitle = item.name ?: binding.root.context.getString(R.string.no_title)
                    binding.tvTitle.text = lastBoundTitle

                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(200)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }

                    val rating = item.voteAverage
                    binding.tvRating.text = if (rating > 0f) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }

                    // Media type with country (use originCountry for TV shows first, then productionCountries)
                    val country = item.originCountry?.firstOrNull()?.let { getCountryFlag(it) }
                        ?: item.productionCountries?.firstOrNull()?.iso?.let { getCountryFlag(it) }
                        ?: ""
                    binding.tvMediaType.text = "$country ${binding.root.context.getString(R.string.media_type_tv_show)}"
                    binding.viewAccentBar.setBackgroundResource(R.drawable.bg_accent_bar_tv)

                    lastBoundOverview = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)
                    binding.tvOverview.text = lastBoundOverview

                    highlightText(binding.tvTitle, lastBoundTitle)
                    highlightText(binding.tvOverview, lastBoundOverview)
                }
            }
        }

        fun updateHighlight() {
            if (lastBoundTitle.isNotEmpty()) highlightText(binding.tvTitle, lastBoundTitle)
            if (lastBoundOverview.isNotEmpty()) highlightText(binding.tvOverview, lastBoundOverview)
        }

        private fun getCountryFlag(iso: String): String {
            return when (iso.uppercase()) {
                "US" -> "🇺🇸"
                "GB", "UK" -> "🇬🇧"
                "UA" -> "🇺🇦"
                "JP" -> "🇯🇵"
                "KR" -> "🇰🇷"
                "FR" -> "🇫🇷"
                "DE" -> "🇩🇪"
                "IT" -> "🇮🇹"
                "ES" -> "🇪🇸"
                "CA" -> "🇨🇦"
                "AU" -> "🇦🇺"
                "IN" -> "🇮🇳"
                "CN" -> "🇨🇳"
                "RU" -> "🇷🇺"
                "BR" -> "🇧🇷"
                "MX" -> "🇲🇽"
                "NZ" -> "🇳🇿"
                "SE" -> "🇸🇪"
                "NO" -> "🇳🇴"
                "DK" -> "🇩🇰"
                "FI" -> "🇫🇮"
                "PL" -> "🇵🇱"
                "NL" -> "🇳🇱"
                "BE" -> "🇧🇪"
                "TR" -> "🇹🇷"
                "TH" -> "🇹🇭"
                "IE" -> "🇮🇪"
                else -> ""
            }
        }

        private fun highlightText(textView: android.widget.TextView, original: String) {
            if (currentQuery.isBlank() || original.isBlank()) {
                textView.text = original
                return
            }
            val idx = original.lowercase().indexOf(currentQuery.lowercase())
            if (idx == -1) {
                textView.text = original
                return
            }
            val span = android.text.SpannableString(original)
            span.setSpan(
                android.text.style.ForegroundColorSpan(textView.context.getColor(R.color.accent)),
                idx,
                idx + currentQuery.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            span.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                idx,
                idx + currentQuery.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = span
        }

        private fun animateRemoval(view: View) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat()),
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f)
                )
                duration = 350
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    // ViewHolder for Person (Actors/Directors)
    inner class PersonViewHolder(private val binding: ItemPersonResultBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { view ->
                view.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .setInterpolator(OvershootInterpolator(2f))
                            .start()
                    }
                    .start()
                    
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(person: Person) {
            binding.tvName.text = person.name

            val photoUrl = person.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
            binding.ivPhoto.load(photoUrl) {
                crossfade(200)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }

            // Department
            val context = binding.root.context
            binding.tvDepartment.text = when (person.knownForDepartment?.lowercase()) {
                "acting" -> context.getString(R.string.department_acting)
                "directing" -> context.getString(R.string.department_directing)
                "writing" -> context.getString(R.string.department_writing)
                "production" -> context.getString(R.string.department_production)
                else -> person.knownForDepartment ?: ""
            }

            // Known for (list of movies/tv shows)
            val knownForTitles = person.knownFor?.mapNotNull { it.title ?: it.name }?.joinToString(", ")
            binding.tvKnownFor.text = knownForTitles ?: ""
            binding.tvKnownFor.visibility = if (knownForTitles.isNullOrEmpty()) View.GONE else View.VISIBLE

        }
    }
}
