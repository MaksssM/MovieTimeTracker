package com.example.movietime.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.databinding.ItemCalendarDayBinding
import java.time.LocalDate

data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val hasEvents: Boolean = false,
    val eventCount: Int = 0,
    val isToday: Boolean = false
)

class CalendarAdapter(
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarDayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
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
        holder.bind(days[position], onDayClick)
    }

    override fun getItemCount(): Int = days.size

    inner class CalendarDayViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay, onDayClick: (Int) -> Unit) {
            val context = binding.root.context
            
            // Set day number
            binding.tvDayNumber.text = day.dayOfMonth.toString()
            
            // Apply styling based on current month
            if (day.isCurrentMonth) {
                binding.tvDayNumber.alpha = 1f
                binding.root.isEnabled = true
            } else {
                binding.tvDayNumber.alpha = 0.3f
                binding.root.isEnabled = false
            }
            
            // Show today highlight (Ring)
            binding.todayHighlight.visibility = if (day.isToday && day.isCurrentMonth) View.VISIBLE else View.GONE
            
            // Show event indicators
            if (day.isCurrentMonth) {
                if (day.eventCount > 0) {
                     binding.eventIndicator.visibility = View.VISIBLE
                     
                     // Optional count badge for many events
                     if (day.eventCount > 2) {
                         binding.tvEventCount.visibility = View.VISIBLE
                         binding.tvEventCount.text = day.eventCount.toString()
                     } else {
                         binding.tvEventCount.visibility = View.GONE
                     }
                } else {
                    binding.eventIndicator.visibility = View.GONE
                    binding.tvEventCount.visibility = View.GONE
                }
            } else {
                binding.eventIndicator.visibility = View.GONE
                binding.tvEventCount.visibility = View.GONE
                binding.todayHighlight.visibility = View.GONE
            }
            
            // Text Color Logic
            if (day.isToday && day.isCurrentMonth) {
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.accent))
                binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                binding.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.tvDayNumber.typeface = android.graphics.Typeface.DEFAULT
            }

            binding.root.setOnClickListener {
                if (day.isCurrentMonth) {
                    com.example.movietime.utils.HapticFeedbackHelper.impactLow(it)
                    onDayClick(day.dayOfMonth)
                }
            }
        }
    }
}
