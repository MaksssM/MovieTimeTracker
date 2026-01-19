package com.example.movietime.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.model.PersonDetails
import com.example.movietime.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonDetailsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _personDetails = MutableStateFlow<PersonDetails?>(null)
    val personDetails: StateFlow<PersonDetails?> = _personDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPersonDetails(personId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load details
                val details = repository.getPersonDetails(personId)
                _personDetails.value = details

                // Load credits (Filmography)
                loadCredits(personId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _actingCredits = MutableStateFlow<List<CreditItem>>(emptyList())
    val actingCredits: StateFlow<List<CreditItem>> = _actingCredits.asStateFlow()

    private val _directingCredits = MutableStateFlow<List<CreditItem>>(emptyList())
    val directingCredits: StateFlow<List<CreditItem>> = _directingCredits.asStateFlow()

    private suspend fun loadCredits(personId: Int) {
        val combinedCredits = repository.getPersonCombinedCredits(personId)
        
        // 1. Acting Credits (Cast)
        val castItems = combinedCredits?.cast?.filter { 
            // Filter out unreleased content (no date)
            (!it.releaseDate.isNullOrBlank() || !it.firstAirDate.isNullOrBlank())
        }?.map { 
             CreditItem(
                id = it.id,
                title = it.title ?: it.name ?: "",
                description = it.character,
                posterPath = it.posterPath,
                mediaType = it.mediaType,
                year = (it.releaseDate ?: it.firstAirDate)?.take(4)
            )
        }?.sortedByDescending { 
            // Sort by popularity if available, otherwise fallback to something else is hard without it. 
            // The API usually returns cast in order, often by popularity or order in list. 
            // We'll rely on API order for now, verifying popularity would require passing it in CreditItem.
            // Actually, let's pass popularity or just assume sorted. 
            // To be safe, let's keep the API order but distinct.
            it.year // Using year as secondary sort or just keeping list order? 
            // Let's filter distinct and keep API order (which is usually relevant).
            // Actually user wants "Known For" vibes, so popularity.
            // Let's rely on the API response order which `combined_credits` provides (usually distinct credits).
            it.year // Just dummy access, keeping original order often best for "Filmography" unless we explicitly sort by date.
            // User asked for "what films he starred in", usually implying chronological or importance.
            // Let's sort by Year Descending (Newest first).
            it.year
        } ?: emptyList()

        // Re-sort by release date descending
        val sortedCast = castItems.sortedByDescending { it.year }

        _actingCredits.value = sortedCast

        // 2. Directing Credits (Crew where department is Directing)
        // Note: Tarantino might have "Screenplay" too, but user asked specifically for "what films he directed".
        val crewItems = combinedCredits?.crew?.filter { 
             // Filter out unreleased content (no date) and ONLY "Directing" department or "Director" job
            ((!it.releaseDate.isNullOrBlank() || !it.firstAirDate.isNullOrBlank()) && 
             it.job == "Director")
        }?.map { 
            CreditItem(
                id = it.id,
                title = it.title ?: it.name ?: "",
                description = it.job,
                posterPath = it.posterPath,
                mediaType = it.mediaType,
                year = (it.releaseDate ?: it.firstAirDate)?.take(4)
            )
        } ?: emptyList()

        val sortedDirecting = crewItems.sortedByDescending { it.year }

        _directingCredits.value = sortedDirecting
    }
}
