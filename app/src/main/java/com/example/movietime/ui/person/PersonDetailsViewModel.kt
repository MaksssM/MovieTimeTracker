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

    private val _movieCredits = MutableStateFlow<List<CreditItem>>(emptyList())
    val movieCredits: StateFlow<List<CreditItem>> = _movieCredits.asStateFlow()

    private val _tvShowCredits = MutableStateFlow<List<CreditItem>>(emptyList())
    val tvShowCredits: StateFlow<List<CreditItem>> = _tvShowCredits.asStateFlow()

    private val _directingCredits = MutableStateFlow<List<CreditItem>>(emptyList())
    val directingCredits: StateFlow<List<CreditItem>> = _directingCredits.asStateFlow()

    private suspend fun loadCredits(personId: Int) {
        val combinedCredits = repository.getPersonCombinedCredits(personId)

        // Genre IDs for non-scripted TV: Talk Show (10767), Reality (10764), News (10763)
        val talkShowGenreIds = setOf(10763, 10764, 10767)

        val allCastRaw = combinedCredits?.cast?.filter {
            (!it.releaseDate.isNullOrBlank() || !it.firstAirDate.isNullOrBlank())
        } ?: emptyList()

        // Movies + scripted TV series — primary focus
        _movieCredits.value = allCastRaw
            .filter { credit ->
                credit.mediaType == "movie" ||
                (credit.mediaType == "tv" && credit.genreIds.none { it in talkShowGenreIds })
            }
            .map { CreditItem(
                id = it.id,
                title = it.title ?: it.name ?: "",
                description = it.character,
                posterPath = it.posterPath,
                mediaType = it.mediaType,
                year = (it.releaseDate ?: it.firstAirDate)?.take(4)
            ) }
            .sortedByDescending { it.year }

        // Talk shows, reality, news — secondary section
        _tvShowCredits.value = allCastRaw
            .filter { credit ->
                credit.mediaType == "tv" && credit.genreIds.any { it in talkShowGenreIds }
            }
            .map { CreditItem(
                id = it.id,
                title = it.title ?: it.name ?: "",
                description = it.character,
                posterPath = it.posterPath,
                mediaType = it.mediaType,
                year = (it.releaseDate ?: it.firstAirDate)?.take(4)
            ) }
            .sortedByDescending { it.year }

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
