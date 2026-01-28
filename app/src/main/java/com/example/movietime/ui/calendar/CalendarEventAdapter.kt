package com.example.movietime.ui.calendar

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Size
import com.example.movietime.R
import com.example.movietime.databinding.ItemCalendarEventBinding
import com.example.movietime.databinding.ItemCalendarReleaseBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarRelease(
    val id: String,
    val title: String,
    val posterUrl: String,
    val releaseDate: LocalDate,
    val rating: Double,
    val isMovie: Boolean
)

data class CalendarEventData(
    val date: LocalDate,
    val releases: List<CalendarRelease>
)

class CalendarEventAdapter(
    private val context: Context,
    private val onReleaseClick: (CalendarRelease) -> Unit
) : ListAdapter<CalendarEventData, CalendarEventAdapter.EventViewHolder>(DiffCallback) {

    companion object {
        // Static formatter to avoid recreation on every bind
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk"))
        
        private val DiffCallback = object : DiffUtil.ItemCallback<CalendarEventData>() {
            override fun areItemsTheSame(oldItem: CalendarEventData, newItem: CalendarEventData) =
                oldItem.date == newItem.date
            override fun areContentsTheSame(oldItem: CalendarEventData, newItem: CalendarEventData) =
                oldItem == newItem
        }
    }

    fun submitEvents(newEvents: List<CalendarEventData>) {
        submitList(newEvents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemCalendarEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), context, onReleaseClick)
    }

    inner class EventViewHolder(private val binding: ItemCalendarEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            eventData: CalendarEventData,
            context: Context,
            onReleaseClick: (CalendarRelease) -> Unit
        ) {
            binding.tvEventDate.text = eventData.date.format(DATE_FORMATTER)
            
            // Show releases count
            binding.tvReleasesCount.text = context.getString(R.string.releases_count, eventData.releases.size)

            binding.eventsContainer.removeAllViews()

            eventData.releases.forEach { release ->
                val releaseBinding = ItemCalendarReleaseBinding.inflate(
                    LayoutInflater.from(context),
                    binding.eventsContainer,
                    false
                )

                releaseBinding.tvTitle.text = release.title
                
                // Update chips instead of TextViews
                releaseBinding.chipType.text = if (release.isMovie) 
                    context.getString(R.string.movie) 
                else 
                    context.getString(R.string.tv_show)
                    
                releaseBinding.chipType.setChipIconResource(
                    if (release.isMovie) R.drawable.ic_movie_24 else R.drawable.ic_tv_24
                )
                
                releaseBinding.chipRating.text = String.format(Locale.US, "⭐ %.1f", release.rating)

                if (release.posterUrl.isNotBlank()) {
                    releaseBinding.ivPoster.load(release.posterUrl) {
                        crossfade(true)
                        size(200, 300)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                }

                releaseBinding.root.setOnClickListener {
                    onReleaseClick(release)
                }

                binding.eventsContainer.addView(releaseBinding.root)
            }
        }
    }
}
