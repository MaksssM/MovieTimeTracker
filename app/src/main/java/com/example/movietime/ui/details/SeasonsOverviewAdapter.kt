package com.example.movietime.ui.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.model.TvSeason
import com.example.movietime.databinding.ItemSeasonOverviewBinding

/**
 * Seasons overview for the TV details screen.
 * Works for any series type (series, cartoons, anime) — data comes from TMDB,
 * regular seasons only (specials live in the progress sheet filter).
 */
class SeasonsOverviewAdapter(
    private val onSeasonClick: (TvSeason) -> Unit
) : ListAdapter<TvSeason, SeasonsOverviewAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TvSeason>() {
            override fun areItemsTheSame(oldItem: TvSeason, newItem: TvSeason): Boolean =
                oldItem.seasonNumber == newItem.seasonNumber

            override fun areContentsTheSame(oldItem: TvSeason, newItem: TvSeason): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSeasonOverviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSeasonOverviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(season: TvSeason) {
            val context = binding.root.context
            val seasonNumber = season.seasonNumber ?: 0

            binding.tvName.text = if (seasonNumber == 0) {
                season.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.season_format, 0)
            } else {
                season.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.season_format, seasonNumber)
            }

            val count = season.episodeCount ?: 0
            binding.tvCount.text = context.getString(R.string.total_episodes_format, count)

            if (!season.posterPath.isNullOrEmpty()) {
                binding.ivPoster.load("https://image.tmdb.org/t/p/w342${season.posterPath}") {
                    crossfade(true)
                    placeholder(R.color.poster_placeholder_dark)
                    error(R.color.poster_placeholder_dark)
                }
            } else {
                binding.ivPoster.setImageResource(R.color.poster_placeholder_dark)
            }

            binding.root.setOnClickListener { onSeasonClick(season) }
        }
    }
}
