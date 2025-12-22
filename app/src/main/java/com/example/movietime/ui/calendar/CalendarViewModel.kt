package com.example.movietime.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _currentMonth = MutableLiveData<YearMonth>(YearMonth.now())
    val currentMonth: LiveData<YearMonth> = _currentMonth

    private val _calendarDays = MutableLiveData<List<CalendarDay>>()
    val calendarDays: LiveData<List<CalendarDay>> = _calendarDays

    private val _events = MutableLiveData<List<CalendarEventData>>()
    val events: LiveData<List<CalendarEventData>> = _events

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val releasesCache = mutableMapOf<LocalDate, List<CalendarRelease>>()

    init {
        loadMonth(YearMonth.now())
    }

    fun nextMonth() {
        val next = _currentMonth.value?.plusMonths(1) ?: YearMonth.now()
        loadMonth(next)
    }

    fun previousMonth() {
        val prev = _currentMonth.value?.minusMonths(1) ?: YearMonth.now()
        loadMonth(prev)
    }

    private fun loadMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
        updateCalendarDays()
        loadReleases(yearMonth)
    }

    private fun updateCalendarDays() {
        val month = _currentMonth.value ?: return
        val days = mutableListOf<CalendarDay>()

        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()

        // Add previous month's days
        val firstDayOfWeek = firstDay.dayOfWeek.value % 7 // 0 = Sunday, 1 = Monday
        for (i in 0 until firstDayOfWeek) {
            val prevDate = firstDay.minusDays((firstDayOfWeek - i).toLong())
            days.add(CalendarDay(prevDate.dayOfMonth, false, false))
        }

        // Add current month's days
        for (day in 1..lastDay.dayOfMonth) {
            val date = month.atDay(day)
            val hasEvents = releasesCache.containsKey(date)
            days.add(CalendarDay(day, true, hasEvents))
        }

        // Add next month's days to fill grid
        val remainingCells = 42 - days.size // 6 rows * 7 days
        for (i in 1..remainingCells) {
            days.add(CalendarDay(i, false, false))
        }

        _calendarDays.value = days
    }

    private fun loadReleases(yearMonth: YearMonth) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch upcoming releases for the month
                val upcomingMovies = repository.getUpcomingMovies()
                val upcomingTv = repository.getUpcomingTvShows()

                val eventsMap = mutableMapOf<LocalDate, MutableList<CalendarRelease>>()

                // Process movies
                for (movie in upcomingMovies) {
                    val dateString = movie.releaseDate ?: continue
                    val releaseDate = try {
                        LocalDate.parse(dateString)
                    } catch (e: Exception) {
                        continue
                    }

                    if (releaseDate.year == yearMonth.year &&
                        releaseDate.monthValue == yearMonth.monthValue
                    ) {
                        val posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" } ?: ""
                        eventsMap.getOrPut(releaseDate) { mutableListOf() }.add(
                            CalendarRelease(
                                id = movie.id.toString(),
                                title = movie.title ?: "",
                                posterUrl = posterUrl,
                                releaseDate = releaseDate,
                                rating = movie.voteAverage.toDouble(),
                                isMovie = true
                            )
                        )
                    }
                }

                // Process TV shows (air dates)
                for (show in upcomingTv) {
                    val dateString = show.firstAirDate ?: continue
                    val releaseDate = try {
                        LocalDate.parse(dateString)
                    } catch (e: Exception) {
                        continue
                    }

                    if (releaseDate.year == yearMonth.year &&
                        releaseDate.monthValue == yearMonth.monthValue
                    ) {
                        val posterUrl = show.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" } ?: ""
                        eventsMap.getOrPut(releaseDate) { mutableListOf() }.add(
                            CalendarRelease(
                                id = show.id.toString(),
                                title = show.name ?: "",
                                posterUrl = posterUrl,
                                releaseDate = releaseDate,
                                rating = show.voteAverage.toDouble(),
                                isMovie = false
                            )
                        )
                    }
                }

                releasesCache.putAll(eventsMap)
                updateCalendarDays()

                _events.value = eventsMap.toSortedMap().map { (date, releases) ->
                    CalendarEventData(date, releases)
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    fun onDayClick(dayOfMonth: Int) {
        val month = _currentMonth.value ?: return
        val date = month.atDay(dayOfMonth)

        releasesCache[date]?.let {
            if (it.isNotEmpty()) {
                _events.value = listOf(CalendarEventData(date, it))
            }
        }
    }
}
