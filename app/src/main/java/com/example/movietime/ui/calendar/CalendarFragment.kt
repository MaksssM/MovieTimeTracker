package com.example.movietime.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.FragmentCalendarBinding
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventAdapter: CalendarEventAdapter

    companion object {
        private const val TAG = "CalendarFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupAdapters()
        setupObservers()
        setupListeners()
    }

    private fun setupAdapters() {
        try {
            Log.d(TAG, "setupAdapters starting...")
            
            calendarAdapter = CalendarAdapter { dayOfMonth ->
                viewModel.onDayClick(dayOfMonth)
            }
            Log.d(TAG, "CalendarAdapter created")
            
            binding.rvCalendarDays.adapter = calendarAdapter
            Log.d(TAG, "CalendarAdapter set to RecyclerView")

            eventAdapter = CalendarEventAdapter(requireContext()) { release ->
                // Handle release click if needed
                Log.d(TAG, "Release clicked: ${release.title}")
            }
            Log.d(TAG, "CalendarEventAdapter created")
            
            binding.rvCalendarEvents.adapter = eventAdapter
            binding.rvCalendarEvents.layoutManager = LinearLayoutManager(requireContext())
            Log.d(TAG, "setupAdapters completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupAdapters: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            updateMonthYear(month)
        }

        viewModel.calendarDays.observe(viewLifecycleOwner) { days ->
            calendarAdapter.submitDays(days)
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            if (events.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvCalendarEvents.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvCalendarEvents.visibility = View.VISIBLE
                eventAdapter.submitEvents(events)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        binding.btnNextMonth.setOnClickListener {
            viewModel.nextMonth()
        }

        binding.btnPrevMonth.setOnClickListener {
            viewModel.previousMonth()
        }
    }

    private fun updateMonthYear(month: YearMonth) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("uk"))
        binding.tvMonthYear.text = month.format(formatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("uk")) else it.toString() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
