package com.example.movietime.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
                val intent = if (release.isMovie) {
                    android.content.Intent(requireContext(), com.example.movietime.ui.details.DetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", release.id.toInt())
                        putExtra("MEDIA_TYPE", "movie")
                    }
                } else {
                    android.content.Intent(requireContext(), com.example.movietime.ui.details.TvDetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", release.id.toInt())
                    }
                }
                startActivity(intent)
            }
            Log.d(TAG, "CalendarEventAdapter created")
            
            binding.rvCalendarEvents.adapter = eventAdapter
            binding.rvCalendarEvents.layoutManager = LinearLayoutManager(requireContext())
            binding.rvCalendarEvents.layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(),
                R.anim.layout_animation_cascade
            )
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
                // Trigger layout animation when events change
                binding.rvCalendarEvents.scheduleLayoutAnimation()
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
        binding.btnNextMonth.setOnClickListener { view ->
            animateButton(view) { viewModel.nextMonth() }
        }

        binding.btnPrevMonth.setOnClickListener { view ->
            animateButton(view) { viewModel.previousMonth() }
        }

        binding.btnToday.setOnClickListener { view ->
            animateButton(view) { viewModel.resetToToday() }
        }
    }
    
    private fun animateButton(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
                action()
            }
            .start()
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
