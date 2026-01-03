package com.example.movietime.ui.statistics

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.movietime.R
import com.example.movietime.databinding.FragmentYearInReviewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import java.util.Calendar

@AndroidEntryPoint
class YearInReviewFragment : Fragment() {

    private var _binding: FragmentYearInReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: YearInReviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYearInReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.selectedYear.observe(viewLifecycleOwner) { year ->
            binding.tvYearTitle.text = "$year Wrapped"
        }

        viewModel.yearlyStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                updateUI(stats)
            } else {
                showEmptyState()
            }
        }

        viewModel.yearlyTrend.observe(viewLifecycleOwner) { trend ->
            updateYearlyChart(trend)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Could show loading indicator here
        }
    }

    private fun updateUI(stats: com.example.movietime.data.db.YearlyStats) {
        with(binding) {
            // Total watch time
            tvTotalWatchTime.text = viewModel.formatWatchTime(stats.totalWatchTimeMinutes)
            tvWatchTimeEquivalent.text = viewModel.formatWatchTimeEquivalent(stats.totalWatchTimeMinutes)

            // Totals
            tvTotalMovies.text = stats.totalMovies.toString()
            tvTotalEpisodes.text = stats.totalTvEpisodes.toString()

            // Top rated item
            if (stats.topRatedItemTitle != null) {
                tvTopRatedTitle.text = stats.topRatedItemTitle
                tvTopRatedRating.text = "${stats.topRatedItemRating?.toInt() ?: 0}/10"
            } else {
                tvTopRatedTitle.text = "Немає даних"
                tvTopRatedRating.text = "-"
            }

            // Most rewatched
            if (stats.mostRewatchedItemTitle != null && stats.mostRewatchedCount > 1) {
                cardMostRewatched.isVisible = true
                tvMostRewatchedTitle.text = stats.mostRewatchedItemTitle
                tvRewatchCount.text = "${stats.mostRewatchedCount} разів"
            } else {
                cardMostRewatched.isVisible = false
            }

            // Longest movie
            if (stats.longestMovieTitle != null) {
                cardLongestMovie.isVisible = true
                tvLongestMovieTitle.text = stats.longestMovieTitle
                tvLongestMovieRuntime.text = "${stats.longestMovieRuntime} хв"
            } else {
                cardLongestMovie.isVisible = false
            }

            // Monthly chart
            stats.monthlyBreakdown?.let { breakdown ->
                updateMonthlyChart(breakdown)
            }
        }
    }

    private fun updateYearlyChart(trend: List<com.example.movietime.data.db.YearlyStats>) {
        val chart = binding.yearlyChart
        if (trend.isEmpty()) {
            binding.yearlyChartContainer.isVisible = false
            return
        }

        val sorted = trend.sortedBy { it.year }
        val labels = sorted.map { it.year.toString() }
        val entries = sorted.mapIndexed { index, stat ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), (stat.totalWatchTimeMinutes / 60f))
        }

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Годин перегляду")
        val primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
        val onSurface = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)

        dataSet.color = primaryColor
        dataSet.lineWidth = 2.5f
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(primaryColor)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = primaryColor
        dataSet.fillAlpha = 60
        dataSet.mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        dataSet.valueTextColor = onSurface
        dataSet.valueTextSize = 9f
        dataSet.setDrawValues(false)

        val lineData = com.github.mikephil.charting.data.LineData(dataSet)

        chart.data = lineData
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(false)

        val xAxis = chart.xAxis
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = onSurface
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f

        val leftAxis = chart.axisLeft
        leftAxis.textColor = onSurface
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = onSurface
        leftAxis.axisLineColor = onSurface
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()} год"
        }

        chart.animateY(900)
        chart.invalidate()

        binding.yearlyChartContainer.isVisible = true
        binding.yearlyChartCard.isVisible = true
    }

    private fun updateMonthlyChart(breakdownJson: String) {
        val chart = binding.monthlyChart
        try {
            val jsonArray = JSONArray(breakdownJson)
            val monthNames = listOf("Січ", "Лют", "Бер", "Кві", "Тра", "Чер", "Лип", "Сер", "Вер", "Жов", "Лис", "Гру")
            val entries = ArrayList<com.github.mikephil.charting.data.BarEntry>()
            
            for (i in 0 until 12) {
                val value = jsonArray.optInt(i, 0)
                entries.add(com.github.mikephil.charting.data.BarEntry(i.toFloat(), value.toFloat()))
            }

            val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Час перегляду (хв)")
            
            // Styling
            val primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
            dataSet.color = primaryColor
            dataSet.valueTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            dataSet.valueTextSize = 10f
            dataSet.setDrawValues(false) // Show values only on click/focus if needed, or keeping clean

            val barData = com.github.mikephil.charting.data.BarData(dataSet)
            barData.barWidth = 0.6f

            chart.data = barData
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.axisRight.isEnabled = false
            
            // X Axis
            val xAxis = chart.xAxis
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(monthNames)
            xAxis.granularity = 1f
            
            // Left Axis
            val leftAxis = chart.axisLeft
            leftAxis.textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            leftAxis.axisLineColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            
            chart.animateY(1000)
            chart.invalidate()

            binding.monthlyChart.isVisible = true
            binding.monthlyChartContainer.isVisible = true

        } catch (e: Exception) {
            chart.isVisible = false
            // Placeholder removed
        }
    }

    private fun showEmptyState() {
        with(binding) {
            tvTotalWatchTime.text = "0 годин"
            tvWatchTimeEquivalent.text = "Почніть дивитися фільми!"
            tvTotalMovies.text = "0"
            tvTotalEpisodes.text = "0"
            tvTopRatedTitle.text = "Немає даних"
            tvTopRatedRating.text = "-"
            cardMostRewatched.isVisible = false
            cardLongestMovie.isVisible = false
        }
    }

    private fun setupClickListeners() {
        binding.btnShare.setOnClickListener {
            shareResults()
        }
    }

    private fun shareResults() {
        val stats = viewModel.yearlyStats.value ?: return
        val year = viewModel.selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR)

        val shareText = buildString {
            appendLine("🎬 Мій Кінорік $year 🎬")
            appendLine()
            appendLine("⏱️ Переглянув: ${viewModel.formatWatchTime(stats.totalWatchTimeMinutes)}")
            appendLine("🎬 Фільмів: ${stats.totalMovies}")
            appendLine("📺 Епізодів: ${stats.totalTvEpisodes}")
            stats.topRatedItemTitle?.let {
                appendLine("⭐ Фаворит: $it")
            }
            appendLine()
            appendLine("#MovieTimeWrapped #КіноРік$year")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Поділитися результатами")
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "YearInReviewFragment"

        fun newInstance(): YearInReviewFragment {
            return YearInReviewFragment()
        }
    }
}
