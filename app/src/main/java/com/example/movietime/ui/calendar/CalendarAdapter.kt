package com.example.movietime.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ItemCalendarDayBinding
import java.time.LocalDate

data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val hasEvents: Boolean = false,
    val eventCount: Int = 0,
    val isToday: Boolean = false,
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isSelected: Boolean = false
)

class CalendarAdapter(
    private val onDayClick: (Int) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.CalendarDayViewHolder>(DiffCallback) {

    private var selectedDay: Int = -1

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CalendarDay>() {
            override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay) =
                oldItem.dayOfMonth == newItem.dayOfMonth && oldItem.isCurrentMonth == newItem.isCurrentMonth
            override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay) =
                oldItem == newItem
        }
    }

    fun submitDays(newDays: List<CalendarDay>) {
        submitList(newDays)
    }

    fun setSelectedDay(day: Int) {
        val oldSelected = selectedDay
        selectedDay = day
        // Refresh old and new selection
        currentList.forEachIndexed { index, calendarDay ->
            if (calendarDay.isCurrentMonth && (calendarDay.dayOfMonth == oldSelected || calendarDay.dayOfMonth == day)) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(getItem(position), onDayClick)
    }

    inner class CalendarDayViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay, onDayClick: (Int) -> Unit) {
            val context = binding.root.context
            
            binding.tvDayNumber.text = day.dayOfMonth.toString()
            
            val isSelected = day.isCurrentMonth && day.dayOfMonth == selectedDay
            
            // Apply styling based on current month
            if (day.isCurrentMonth) {
                binding.tvDayNumber.alpha = 1f
                binding.root.isEnabled = true
            } else {
                binding.tvDayNumber.alpha = 0.25f
                binding.root.isEnabled = false
            }
            
            // Show selected background
            binding.dayBackground.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            
            // Show today highlight (ring) only when not selected
            binding.todayHighlight.visibility = if (day.isToday && day.isCurrentMonth && !isSelected) View.VISIBLE else View.GONE
            
            // Show movie/TV dots
            if (day.isCurrentMonth && day.eventCount > 0) {
                binding.dotMovie.visibility = if (day.movieCount > 0) View.VISIBLE else View.GONE
                binding.dotTv.visibility = if (day.tvCount > 0) View.VISIBLE else View.GONE
                binding.eventIndicator.visibility = View.GONE
                
                // Count badge for many events
                if (day.eventCount > 2) {
                    binding.tvEventCount.visibility = View.VISIBLE
                    binding.tvEventCount.text = day.eventCount.toString()
                } else {
                    binding.tvEventCount.visibility = View.GONE
                }
            } else {
                binding.dotMovie.visibility = View.GONE
                binding.dotTv.visibility = View.GONE
                binding.eventIndicator.visibility = View.GONE
                binding.tvEventCount.visibility = View.GONE
            }
            
            if (!day.isCurrentMonth) {
                binding.todayHighlight.visibility = View.GONE
                binding.tvEventCount.visibility = View.GONE
            }
            
            // Text Color Logic
            when {
                isSelected -> {
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                day.isToday && day.isCurrentMonth -> {
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.accent))
                    binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                day.hasEvents && day.isCurrentMonth -> {
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                else -> {
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT
                }
            }

            binding.root.setOnClickListener {
                if (day.isCurrentMonth) {
                    com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
                    setSelectedDay(day.dayOfMonth)
                    it.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(80)
                        .withEndAction {
                            it.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                                .start()
                        }
                        .start()
                    onDayClick(day.dayOfMonth)
                }
            }
        }
    }
}
