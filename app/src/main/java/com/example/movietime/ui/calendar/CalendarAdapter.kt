package com.example.movietime.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.databinding.ItemCalendarDayBinding
import java.time.YearMonth

data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val hasEvents: Boolean = false
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
            binding.tvDayNumber.text = day.dayOfMonth.toString()
            binding.tvDayNumber.alpha = if (day.isCurrentMonth) 1f else 0.3f
            binding.eventIndicator.visibility =
                if (day.hasEvents && day.isCurrentMonth) android.view.View.VISIBLE
                else android.view.View.GONE

            binding.root.setOnClickListener {
                if (day.isCurrentMonth) {
                    onDayClick(day.dayOfMonth)
                }
            }
        }
    }
}
