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
import java.util.Locale

// Групований адаптер для поділу результатів
data class GroupedSearchItem(
    val type: ItemType,
    val data: Any
) {
    enum class ItemType {
        HEADER, MOVIE, TV_SHOW
    }
}

class GroupedSearchAdapter : ListAdapter<GroupedSearchItem, RecyclerView.ViewHolder>(GroupedDiffCallback) {

    var onItemClick: ((Any) -> Unit)? = null
    var currentQuery: String = ""

    fun updateQueryHighlight(q: String) {
        currentQuery = q.trim()
        for (i in 0 until itemCount) {
            notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GROUP_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                HeaderViewHolder(view)
            }
            else -> SearchViewHolder(
                ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SearchViewHolder -> holder.bind(getItem(position).data)
            is HeaderViewHolder -> holder.bind(getItem(position).data as String)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            GroupedSearchItem.ItemType.HEADER -> GROUP_HEADER
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
                    binding.tvTitle.text = item.title ?: item.name ?: "Без назви"
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                    val rating = item.voteAverage ?: 0.0
                    binding.tvRating.text = if (rating > 0) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }
                    binding.tvMediaType.text = "🎬 Фільм"
                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: "Опис не доступний"
                }
                is TvShowResult -> {
                    binding.tvTitle.text = item.name ?: item.title ?: "Без назви"
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                    val rating = item.voteAverage ?: 0.0
                    binding.tvRating.text = if (rating > 0) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }
                    binding.tvMediaType.text = "📺 Серіал"
                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: "Опис не доступний"
                }
            }
        }
    }

    class HeaderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: String) {
            itemView.findViewById<android.widget.TextView>(R.id.tvHeaderTitle).text = title
        }
    }

    companion object {
        private const val GROUP_HEADER = 0
        private const val ITEM_CONTENT = 1

        private val GroupedDiffCallback = object : DiffUtil.ItemCallback<GroupedSearchItem>() {
            override fun areItemsTheSame(oldItem: GroupedSearchItem, newItem: GroupedSearchItem): Boolean {
                return when {
                    oldItem.type == GroupedSearchItem.ItemType.HEADER && newItem.type == GroupedSearchItem.ItemType.HEADER -> true
                    oldItem.data is MovieResult && newItem.data is MovieResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is TvShowResult && newItem.data is TvShowResult -> oldItem.data.id == newItem.data.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: GroupedSearchItem, newItem: GroupedSearchItem): Boolean {
                return when {
                    oldItem.data is MovieResult && newItem.data is MovieResult -> oldItem.data.id == newItem.data.id
                    oldItem.data is TvShowResult && newItem.data is TvShowResult -> oldItem.data.id == newItem.data.id
                    else -> oldItem == newItem
                }
            }
        }
    }
}

