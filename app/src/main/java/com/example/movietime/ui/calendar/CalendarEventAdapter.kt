package com.example.movietime.ui.calendar

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
) : RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {

    private var events: List<CalendarEventData> = emptyList()

    fun submitEvents(newEvents: List<CalendarEventData>) {
        events = newEvents
        notifyDataSetChanged()
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
        holder.bind(events[position], context, onReleaseClick)
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(private val binding: ItemCalendarEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            eventData: CalendarEventData,
            context: Context,
            onReleaseClick: (CalendarRelease) -> Unit
        ) {
            // Format date with Ukrainian locale
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk"))
            binding.tvEventDate.text = eventData.date.format(formatter)
            
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
                releaseBinding.tvRating.text = String.format(Locale.US, "%.1f", release.rating)
                
                // Set type text and icon
                releaseBinding.tvType.text = if (release.isMovie) 
                    context.getString(R.string.movie) 
                else 
                    context.getString(R.string.tv_show)
                    
                releaseBinding.ivTypeIcon.setImageResource(
                    if (release.isMovie) R.drawable.ic_movie_24 else R.drawable.ic_tv_24
                )

                if (release.posterUrl.isNotBlank()) {
                    releaseBinding.ivPoster.load(release.posterUrl) {
                        crossfade(true)
                        placeholder(R.color.shimmer_base)
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
