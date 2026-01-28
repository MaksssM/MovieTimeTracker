package com.example.movietime.ui.today.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.movietime.R
import com.example.movietime.data.model.TodayReleaseItem
import com.example.movietime.databinding.ItemTodayReleaseBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class TodayReleaseAdapter(
    private val onItemClick: (TodayReleaseItem) -> Unit
) : ListAdapter<TodayReleaseItem, TodayReleaseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayReleaseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTodayReleaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: TodayReleaseItem) {
            val context = binding.root.context
            
            binding.tvTitle.text = item.title
            
            // Media type badge
            binding.tvMediaType.text = if (item.mediaType == "tv") {
                context.getString(R.string.tv_show_short)
            } else {
                context.getString(R.string.movie_short)
            }

            // Rating
            if (item.voteAverage > 0) {
                binding.tvRating.text = String.format(Locale.US, "%.1f", item.voteAverage)
            } else {
                binding.tvRating.text = "—"
            }

            // Release date info
            binding.tvReleaseInfo.text = formatReleaseDate(item.releaseDate, context)

            // Load poster
            val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
            
            binding.ivPoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_poster)
                error(R.drawable.placeholder_poster)
                transformations(RoundedCornersTransformation(16f))
            }
        }

        private fun formatReleaseDate(dateStr: String?, context: android.content.Context): String {
            if (dateStr.isNullOrBlank()) return ""
            
            return try {
                val releaseDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                val today = LocalDate.now()
                val daysDiff = ChronoUnit.DAYS.between(today, releaseDate)
                
                when {
                    daysDiff == 0L -> context.getString(R.string.today).uppercase()
                    daysDiff == 1L -> context.getString(R.string.tomorrow).uppercase()
                    daysDiff in 2..7 -> context.getString(R.string.in_days, daysDiff.toInt())
                    else -> {
                        val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                        formatter.format(java.sql.Date.valueOf(dateStr))
                    }
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TodayReleaseItem>() {
        override fun areItemsTheSame(oldItem: TodayReleaseItem, newItem: TodayReleaseItem): Boolean {
            return oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType
        }

        override fun areContentsTheSame(oldItem: TodayReleaseItem, newItem: TodayReleaseItem): Boolean {
            return oldItem == newItem
        }
    }
}
