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

class SearchAdapter : ListAdapter<Any, SearchAdapter.SearchViewHolder>(DiffCallback) {

    var onItemClick: ((Any) -> Unit)? = null
    var onItemLongClick: ((Any) -> Unit)? = null
    var currentQuery: String = ""

    fun updateQueryHighlight(q: String) {
        currentQuery = q.trim()
        // Обновляем только видимые элементы
        for (i in 0 until itemCount) {
            notifyItemChanged(i)
        }
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < itemCount) {
            val currentList = currentList.toMutableList()
            currentList.removeAt(position)
            submitList(currentList)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class SearchViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position))
                    // Анімація видалення
                    animateRemoval(binding.root)
                    removeItem(position)
                }
                true
            }
        }

        fun bind(item: Any) {
            when (item) {
                is MovieResult -> {
                    // Назва фільму
                    binding.tvTitle.text = item.title ?: binding.root.context.getString(R.string.no_title)

                    // Постер із завантаженням та кешуванням
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }

                    // Рейтинг
                    val rating = item.voteAverage
                    binding.tvRating.text = if (rating > 0f) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }

                    // Тип медіа
                    binding.tvMediaType.text = binding.root.context.getString(R.string.media_type_movie)

                    // Опис
                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)

                    highlightText(binding.tvTitle, binding.tvTitle.text.toString())
                    highlightText(binding.tvOverview, binding.tvOverview.text.toString())
                }
                is TvShowResult -> {
                    // Назва серіалу
                    binding.tvTitle.text = item.name ?: binding.root.context.getString(R.string.no_title)

                    // Постер із завантаженням та кешуванням
                    val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    binding.ivPoster.load(posterUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }

                    // Рейтинг
                    val rating = item.voteAverage
                    binding.tvRating.text = if (rating > 0f) {
                        String.format(Locale.US, "★ %.1f", rating)
                    } else {
                        "★ N/A"
                    }

                    // Тип медіа
                    binding.tvMediaType.text = binding.root.context.getString(R.string.media_type_tv_show)

                    // Опис
                    binding.tvOverview.text = item.overview?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.no_description_available)

                    highlightText(binding.tvTitle, binding.tvTitle.text.toString())
                    highlightText(binding.tvOverview, binding.tvOverview.text.toString())
                }
            }

            // Анімація появи елемента
            itemView.alpha = 0f
            itemView.scaleX = 0.95f
            itemView.scaleY = 0.95f
            itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(0)
                .start()
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

        private fun animateRemoval(view: android.view.View) {
            view.animate()
                .alpha(0f)
                .translationX(-view.width.toFloat())
                .setDuration(300)
                .start()
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> {
                        oldItem.id == newItem.id
                    }
                    oldItem is TvShowResult && newItem is TvShowResult -> {
                        oldItem.id == newItem.id
                    }
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is MovieResult && newItem is MovieResult -> {
                        oldItem.id == newItem.id &&
                                oldItem.title == newItem.title &&
                                oldItem.posterPath == newItem.posterPath &&
                                oldItem.voteAverage == newItem.voteAverage
                    }
                    oldItem is TvShowResult && newItem is TvShowResult -> {
                        oldItem.id == newItem.id &&
                                oldItem.name == newItem.name &&
                                oldItem.posterPath == newItem.posterPath &&
                                oldItem.voteAverage == newItem.voteAverage
                    }
                    else -> false
                }
            }
        }
    }
}

