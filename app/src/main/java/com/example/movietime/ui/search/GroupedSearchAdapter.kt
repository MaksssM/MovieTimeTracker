package com.example.movietime.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.databinding.ItemSearchResultBinding
import com.example.movietime.databinding.ItemPersonResultBinding
import com.example.movietime.data.model.Person
import android.view.View
import android.view.animation.OvershootInterpolator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import java.util.Locale

// Групований адаптер для поділу результатів
data class GroupedSearchItem(
    val type: ItemType,
    val data: Any
) {
    enum class ItemType {
        HEADER, MOVIE, TV_SHOW, PERSON
    }
}

class GroupedSearchAdapter(
    private val onItemClickListener: (Any) -> Unit
) : ListAdapter<GroupedSearchItem, RecyclerView.ViewHolder>(GroupedDiffCallback) {

    var onItemClick: ((Any) -> Unit)? = onItemClickListener
    var currentQuery: String = ""

    fun updateItems(items: List<GroupedSearchItem>) {
        submitList(items)
    }

    fun updateQueryHighlight(q: String) {
        currentQuery = q.trim()
        for (i in 0 until itemCount) {
            notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GROUP_HEADER -> {
                // Skip rendering headers; return a zero-height view holder
                val spacer = android.view.View(parent.context).apply { layoutParams = ViewGroup.LayoutParams(0, 0) }
                HeaderViewHolder(spacer)
            }
            VIEW_TYPE_PERSON -> PersonViewHolder(
                ItemPersonResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> SearchViewHolder(
                ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SearchViewHolder -> holder.bind(getItem(position).data)
            is PersonViewHolder -> holder.bind(getItem(position).data as Person)
            is HeaderViewHolder -> { /* headers hidden */ }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            GroupedSearchItem.ItemType.HEADER -> GROUP_HEADER
            GroupedSearchItem.ItemType.PERSON -> VIEW_TYPE_PERSON
            else -> ITEM_CONTENT
        }
    }

    inner class SearchViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position).data)
                }
            }
        }

        fun bind(item: Any) {
            when (item) {
                is MovieResult -> {
                    binding.tvTitle.text = item.title ?: binding.root.context.getString(R.string.no_title)
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
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
                    // Fallback to originalLanguage if productionCountries is missing (common in search results)
                    val countryFlag = item.productionCountries?.firstOrNull()?.iso?.let { getCountryFlag(it) }
                        ?: item.originalLanguage?.let { getLanguageFlag(it) } 
                        ?: ""
                        
                    binding.tvMediaType.text = if (countryFlag.isNotBlank()) "$countryFlag ${binding.root.context.getString(R.string.media_type_movie)}" else binding.root.context.getString(R.string.media_type_movie)

                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)
                }
                is TvShowResult -> {
                    binding.tvTitle.text = item.name ?: binding.root.context.getString(R.string.no_title)
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
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
                    binding.tvMediaType.text = if (country.isNotBlank()) "$country ${binding.root.context.getString(R.string.media_type_tv_show)}" else binding.root.context.getString(R.string.media_type_tv_show)

                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)
                }
            }
        }

        private fun getLanguageFlag(language: String): String {
            return when (language.lowercase()) {
                "en" -> "🇺🇸" // Default to US flag for English
                "uk" -> "🇺🇦"
                "ja" -> "🇯🇵"
                "ko" -> "🇰🇷"
                "fr" -> "🇫🇷"
                "de" -> "🇩🇪"
                "it" -> "🇮🇹"
                "es" -> "🇪🇸"
                "zh", "cn" -> "🇨🇳"
                "ru" -> "🇷🇺"
                "hi" -> "🇮🇳"
                "pt" -> "🇧🇷"
                else -> ""
            }
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
    }

    class HeaderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        fun bind(@Suppress("UNUSED_PARAMETER") title: String) { /* no-op, header hidden */ }
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
                    onItemClick?.invoke(getItem(position).data)
                }
            }
        }

        fun bind(person: Person) {
            binding.tvName.text = person.name

            val photoUrl = person.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" }
            binding.ivPhoto.load(photoUrl) {
                crossfade(400)
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
            // User requested to remove this from search results and only show in details
            binding.tvKnownFor.visibility = View.GONE
        }
    }

    companion object {
        private const val GROUP_HEADER = 0
        private const val ITEM_CONTENT = 1
        private const val VIEW_TYPE_PERSON = 2

        private val GroupedDiffCallback = object : DiffUtil.ItemCallback<GroupedSearchItem>() {
            override fun areItemsTheSame(oldItem: GroupedSearchItem, newItem: GroupedSearchItem): Boolean {
                return when {
                    oldItem.type == GroupedSearchItem.ItemType.HEADER && newItem.type == GroupedSearchItem.ItemType.HEADER -> true
                    oldItem.data is MovieResult && newItem.data is MovieResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is TvShowResult && newItem.data is TvShowResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is Person && newItem.data is Person -> oldItem.data.id == newItem.data.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: GroupedSearchItem, newItem: GroupedSearchItem): Boolean {
                return when {
                    oldItem.data is MovieResult && newItem.data is MovieResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is TvShowResult && newItem.data is TvShowResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is Person && newItem.data is Person -> oldItem.data.id == newItem.data.id
                    else -> oldItem == newItem
                }
            }
        }
    }
}
