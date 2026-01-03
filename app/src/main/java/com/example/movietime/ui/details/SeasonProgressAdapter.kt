package com.example.movietime.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ItemSeasonProgressBinding
import com.example.movietime.util.Utils
import com.example.movietime.utils.HapticFeedbackHelper


/**
 * Дані сезону для відображення в UI
 */
data class SeasonUiModel(
    val seasonNumber: Int,
    val name: String,
    val episodes: MutableList<EpisodeUiModel>,
    var isExpanded: Boolean = false
) {
    val watchedCount: Int get() = episodes.count { it.isWatched }
    val totalCount: Int get() = episodes.size
    val isComplete: Boolean get() = watchedCount == totalCount && totalCount > 0
    val progressPercent: Int get() = if (totalCount > 0) (watchedCount * 100) / totalCount else 0
    val totalRuntime: Int get() = episodes.sumOf { it.runtime ?: 0 }
    val watchedRuntime: Int get() = episodes.filter { it.isWatched }.sumOf { it.runtime ?: 0 }
}

class SeasonProgressAdapter(
    private val onSeasonChecked: (SeasonUiModel, Boolean) -> Unit,
    private val onEpisodeChecked: (SeasonUiModel, EpisodeUiModel, Boolean) -> Unit
) : ListAdapter<SeasonUiModel, SeasonProgressAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSeasonProgressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSeasonProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var episodeAdapter: EpisodeProgressAdapter? = null

        fun bind(season: SeasonUiModel) {
            binding.apply {
                val context = root.context
                
                // Season title
                tvSeasonTitle.text = if (season.seasonNumber == 0) {
                    context.getString(R.string.specials)
                } else {
                    context.getString(R.string.season_format, season.seasonNumber)
                }
                
                // Season info - episodes count
                tvSeasonInfo.text = "${season.watchedCount} / ${season.totalCount} серій"
                
                // Season time
                val watchedTime = Utils.formatMinutesToHoursAndMinutes(season.watchedRuntime)
                tvSeasonTime.text = watchedTime
                
                // Circular progress & percent text
                progressSeason.setProgressCompat(season.progressPercent, false)
                tvProgressPercent.text = "${season.progressPercent}%"
                
                // Checkbox state (without triggering listener)
                cbSeason.setOnCheckedChangeListener(null)
                cbSeason.isChecked = season.isComplete
                
                // Expand/collapse state
                rvEpisodes.visibility = if (season.isExpanded) View.VISIBLE else View.GONE
                divider.visibility = if (season.isExpanded) View.VISIBLE else View.GONE
                ivExpandArrow.rotation = if (season.isExpanded) 180f else 0f
                
                // Setup episodes adapter
                if (episodeAdapter == null) {
                    episodeAdapter = EpisodeProgressAdapter { episode, isChecked ->
                        onEpisodeChecked(season, episode, isChecked)
                        updateSeasonState(season)
                    }
                    rvEpisodes.adapter = episodeAdapter
                }
                episodeAdapter?.submitList(season.episodes.toList())
                
                // Click listeners
                cbSeason.setOnCheckedChangeListener { _, isChecked ->
                    com.example.movietime.utils.HapticFeedbackHelper.impactMedium(cbSeason)
                    // Mark all episodes as watched/unwatched
                    season.episodes.forEach { it.isWatched = isChecked }
                    episodeAdapter?.notifyDataSetChanged()
                    onSeasonChecked(season, isChecked)
                    updateSeasonInfo(season)
                }
                
                seasonHeader.setOnClickListener {
                    com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
                    // Toggle expand/collapse
                    season.isExpanded = !season.isExpanded
                    
                    // Animate arrow
                    val targetRotation = if (season.isExpanded) 180f else 0f
                    ivExpandArrow.animate()
                        .rotation(targetRotation)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                    
                    // Show/hide divider and episodes
                    divider.visibility = if (season.isExpanded) View.VISIBLE else View.GONE
                    
                    if (season.isExpanded) {
                        rvEpisodes.visibility = View.VISIBLE
                        rvEpisodes.alpha = 0f
                        rvEpisodes.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    } else {
                        rvEpisodes.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction { rvEpisodes.visibility = View.GONE }
                            .start()
                    }
                }
            }
        }

        private fun updateSeasonState(season: SeasonUiModel) {
            binding.cbSeason.setOnCheckedChangeListener(null)
            binding.cbSeason.isChecked = season.isComplete
            binding.cbSeason.setOnCheckedChangeListener { _, isChecked ->
                season.episodes.forEach { it.isWatched = isChecked }
                episodeAdapter?.notifyDataSetChanged()
                onSeasonChecked(season, isChecked)
                updateSeasonInfo(season)
            }
            updateSeasonInfo(season)
        }

        private fun updateSeasonInfo(season: SeasonUiModel) {
            val watchedTime = Utils.formatMinutesToHoursAndMinutes(season.watchedRuntime)
            binding.tvSeasonInfo.text = "${season.watchedCount} / ${season.totalCount} серій"
            binding.tvSeasonTime.text = watchedTime
            binding.tvProgressPercent.text = "${season.progressPercent}%"
            
            // Animate circular progress change
            binding.progressSeason.setProgressCompat(season.progressPercent, true)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SeasonUiModel>() {
        override fun areItemsTheSame(oldItem: SeasonUiModel, newItem: SeasonUiModel): Boolean {
            return oldItem.seasonNumber == newItem.seasonNumber
        }

        override fun areContentsTheSame(oldItem: SeasonUiModel, newItem: SeasonUiModel): Boolean {
            return oldItem.watchedCount == newItem.watchedCount && 
                   oldItem.isExpanded == newItem.isExpanded
        }
    }
}
