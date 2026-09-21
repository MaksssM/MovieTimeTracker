package com.example.movietime.ui.randomizer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.movietime.R
import com.example.movietime.data.db.PlannedItem
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.repository.AppRepository
import com.example.movietime.databinding.BottomSheetRandomizerBinding
import com.example.movietime.service.RecommendationService
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RandomMovieCandidate(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val rating: Double?,
    val mediaType: String,
    val releaseYear: String?,
    val runtimeOrEpisodes: String?,
    val genreIds: List<Int> = emptyList()
)

@AndroidEntryPoint
class RandomizerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRandomizerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var repository: AppRepository

    @Inject
    lateinit var recommendationService: RecommendationService

    private var currentCandidates: List<RandomMovieCandidate> = emptyList()
    private var selectedCandidate: RandomMovieCandidate? = null
    private var candidateLoadVersion = 0
    private var isSpinning = false

    private enum class CandidateSource {
        PLANNED,
        RECOMMENDATIONS,
        TOP_RATED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRandomizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        determineInitialSourceAndLoad()
    }

    private fun determineInitialSourceAndLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            val plannedCount = withContext(Dispatchers.IO) {
                try { repository.getTotalPlannedCount() } catch (_: Exception) { 0 }
            }
            if (_binding == null) return@launch
            if (plannedCount > 0) {
                binding.chipPlanned.isChecked = true
            } else {
                binding.chipRecommendations.isChecked = true
            }
            loadCandidatesForCurrentFilters()
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.chipGroupSource.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                loadCandidatesForCurrentFilters()
            }
        }

        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                loadCandidatesForCurrentFilters()
            }
        }

        binding.chipGroupGenre.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                loadCandidatesForCurrentFilters()
            }
        }

        binding.btnSpin.setOnClickListener {
            spinWheel()
        }

        binding.btnViewDetails.setOnClickListener {
            selectedCandidate?.let { candidate ->
                val intent = if (candidate.mediaType == "tv") {
                    Intent(requireContext(), TvDetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", candidate.id)
                        putExtra("MEDIA_TYPE", "tv")
                    }
                } else {
                    Intent(requireContext(), DetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", candidate.id)
                        putExtra("MEDIA_TYPE", "movie")
                    }
                }
                startActivity(intent)
                dismiss()
            }
        }
    }

    private fun getSelectedGenreIds(): List<Int>? {
        val safeBinding = _binding ?: return null
        return when {
            safeBinding.chipGenreAction.isChecked -> listOf(28, 12, 10759)
            safeBinding.chipGenreComedy.isChecked -> listOf(35)
            safeBinding.chipGenreDrama.isChecked -> listOf(18)
            safeBinding.chipGenreScifi.isChecked -> listOf(878, 14, 10765)
            safeBinding.chipGenreHorror.isChecked -> listOf(27)
            safeBinding.chipGenreThriller.isChecked -> listOf(53, 9648)
            safeBinding.chipGenreAnimation.isChecked -> listOf(16)
            safeBinding.chipGenreRomance.isChecked -> listOf(10749, 10766)
            safeBinding.chipGenreCrime.isChecked -> listOf(80)
            safeBinding.chipGenreFamily.isChecked -> listOf(10751)
            else -> null // All
        }
    }

    private fun getSelectedMediaType(): String {
        val safeBinding = _binding ?: return "all"
        return when {
            safeBinding.chipTypeMovies.isChecked -> "movie"
            safeBinding.chipTypeTv.isChecked -> "tv"
            else -> "all"
        }
    }

    private fun loadCandidatesForCurrentFilters() {
        val safeBinding = _binding ?: return
        val source = when {
            safeBinding.chipRecommendations.isChecked -> CandidateSource.RECOMMENDATIONS
            safeBinding.chipTopRated.isChecked -> CandidateSource.TOP_RATED
            else -> CandidateSource.PLANNED
        }
        val mediaType = getSelectedMediaType()
        val genreIds = getSelectedGenreIds()
        val loadVersion = ++candidateLoadVersion

        selectedCandidate = null
        currentCandidates = emptyList()
        safeBinding.layoutMovieResult.isVisible = false
        safeBinding.layoutPlaceholder.isVisible = true
        safeBinding.btnViewDetails.isVisible = false
        safeBinding.btnSpin.isEnabled = false
        safeBinding.btnSpin.text = getString(R.string.loading)

        viewLifecycleOwner.lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                loadCandidates(source, mediaType, genreIds)
            }
            val currentBinding = _binding ?: return@launch
            if (loadVersion != candidateLoadVersion) return@launch

            currentCandidates = candidates
            currentBinding.btnSpin.isEnabled = true
            currentBinding.btnSpin.text = getString(R.string.spin_wheel)

            if (candidates.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_random_items, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadCandidates(
        source: CandidateSource,
        targetMediaType: String,
        genreIds: List<Int>?
    ): List<RandomMovieCandidate> = try {
        val rawCandidates: List<RandomMovieCandidate> = when (source) {
            CandidateSource.PLANNED -> {
                val planned = repository.getPlannedItemsSync().map { it.toRandomCandidate() }
                if (planned.isEmpty()) {
                    loadFromDiscover(targetMediaType, genreIds)
                } else {
                    planned
                }
            }

            CandidateSource.RECOMMENDATIONS -> {
                val recommendations = recommendationService.getPersonalizedRecommendations()
                val list = (recommendations.movies.map { it.toRandomCandidate() } +
                        recommendations.tvShows.map { it.toRandomCandidate() }).shuffled()
                list.ifEmpty { loadFromDiscover(targetMediaType, genreIds) }
            }

            CandidateSource.TOP_RATED -> {
                loadFromDiscover(targetMediaType, genreIds)
            }
        }

        // 1. Фільтрація за типом медіа (all, movie, tv)
        var filtered = when (targetMediaType) {
            "movie" -> rawCandidates.filter { it.mediaType == "movie" }
            "tv" -> rawCandidates.filter { it.mediaType == "tv" }
            else -> rawCandidates
        }

        // 2. Фільтрація за жанром, якщо вибрано
        if (!genreIds.isNullOrEmpty()) {
            val genreFiltered = filtered.filter { candidate ->
                candidate.genreIds.any { it in genreIds }
            }

            if (genreFiltered.isNotEmpty()) {
                filtered = genreFiltered
            } else if (source != CandidateSource.TOP_RATED) {
                // Якщо в локальних списках немає цього жанру, підвантажуємо з онлайн discover
                val discoverMatches = loadFromDiscover(targetMediaType, genreIds)
                if (discoverMatches.isNotEmpty()) {
                    filtered = discoverMatches
                }
            }
        }

        filtered.shuffled()
    } catch (_: Exception) {
        loadFromDiscover(targetMediaType, genreIds)
    }

    private suspend fun loadFromDiscover(
        targetMediaType: String,
        genreIds: List<Int>?
    ): List<RandomMovieCandidate> {
        return try {
            val results = repository.discoverByFilters(
                mediaType = targetMediaType,
                genreIds = genreIds,
                sortBy = "popularity.desc"
            )
            val seenIds = repository.getAllSeenItemIds()
                .map { "${it.id}_${it.mediaType}" }
                .toSet()

            val candidates = results.mapNotNull { item ->
                when (item) {
                    is MovieResult -> if ("${item.id}_movie" !in seenIds) item.toRandomCandidate() else null
                    is TvShowResult -> if ("${item.id}_tv" !in seenIds) item.toRandomCandidate() else null
                    else -> null
                }
            }

            if (candidates.isEmpty()) {
                results.mapNotNull { item ->
                    when (item) {
                        is MovieResult -> item.toRandomCandidate()
                        is TvShowResult -> item.toRandomCandidate()
                        else -> null
                    }
                }
            } else {
                candidates
            }.take(50).shuffled()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun PlannedItem.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = title,
        posterPath = posterPath,
        rating = null,
        mediaType = mediaType,
        releaseYear = releaseDate?.take(4),
        runtimeOrEpisodes = (runtime ?: 0).takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" }
    )

    private fun WatchedItem.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = title,
        posterPath = posterPath,
        rating = userRating?.toDouble() ?: voteAverage,
        mediaType = mediaType,
        releaseYear = releaseDate?.take(4),
        runtimeOrEpisodes = when {
            mediaType == "tv" && (totalEpisodes ?: 0) > 0 ->
                "$totalEpisodes ${getString(R.string.episodes)}"
            mediaType == "tv" -> getString(R.string.tv_show_short)
            (runtime ?: 0) > 0 -> {
                val minutes = runtime ?: 0
                "${minutes / 60}h ${minutes % 60}m"
            }
            else -> null
        },
        genreIds = genreIds?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
    )

    private fun MovieResult.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = title ?: getString(R.string.no_title),
        posterPath = posterPath,
        rating = voteAverage.toDouble(),
        mediaType = "movie",
        releaseYear = releaseDate?.take(4),
        runtimeOrEpisodes = runtime?.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" },
        genreIds = genreIds ?: genres?.map { it.id } ?: emptyList()
    )

    private fun TvShowResult.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = name ?: getString(R.string.no_title),
        posterPath = posterPath,
        rating = voteAverage.toDouble(),
        mediaType = "tv",
        releaseYear = firstAirDate?.take(4),
        runtimeOrEpisodes = numberOfEpisodes?.takeIf { it > 0 }
            ?.let { "$it ${getString(R.string.episodes)}" },
        genreIds = genreIds ?: genres?.map { it.id } ?: emptyList()
    )

    private fun spinWheel() {
        val safeBinding = _binding ?: return
        if (isSpinning) return

        if (currentCandidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_random_items, Toast.LENGTH_SHORT).show()
            loadCandidatesForCurrentFilters()
            return
        }

        isSpinning = true
        safeBinding.btnSpin.isEnabled = false

        // Slot-machine roulette effect
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val shuffleCount = if (currentCandidates.size > 3) 5 else 2
                for (i in 0 until shuffleCount) {
                    val temp = currentCandidates.random()
                    _binding?.let { b ->
                        displayCandidate(temp, isFinal = false)
                    }
                    kotlinx.coroutines.delay(80L + (i * 30L))
                }

                val finalChoice = currentCandidates.random()
                selectedCandidate = finalChoice

                _binding?.let { b ->
                    b.cardResult.animate()
                        .scaleX(0.92f)
                        .scaleY(0.92f)
                        .alpha(0.6f)
                        .setDuration(100)
                        .withEndAction {
                            val cb = _binding ?: return@withEndAction
                            displayCandidate(finalChoice, isFinal = true)
                            cb.cardResult.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(260)
                                .setInterpolator(OvershootInterpolator(2.0f))
                                .start()
                        }
                        .start()
                }
            } finally {
                isSpinning = false
                _binding?.btnSpin?.isEnabled = true
            }
        }
    }

    private fun displayCandidate(candidate: RandomMovieCandidate, isFinal: Boolean = true) {
        val safeBinding = _binding ?: return
        safeBinding.layoutPlaceholder.isVisible = false
        safeBinding.layoutMovieResult.isVisible = true
        safeBinding.btnViewDetails.isVisible = isFinal
        if (isFinal) {
            safeBinding.btnSpin.text = getString(R.string.spin_again)
        }

        safeBinding.tvTitle.text = candidate.title
        safeBinding.tvMediaTypeBadge.text = if (candidate.mediaType == "tv") {
            getString(R.string.tv_show_short)
        } else {
            getString(R.string.movie_short)
        }
        safeBinding.tvMediaTypeBadge.setBackgroundResource(
            if (candidate.mediaType == "tv") R.drawable.bg_badge_tv else R.drawable.bg_badge_movie
        )

        candidate.rating?.let { rating ->
            if (rating > 0) {
                safeBinding.layoutRatingBadge.isVisible = true
                safeBinding.tvRating.text = String.format(java.util.Locale.US, "%.1f", rating)
            } else {
                safeBinding.layoutRatingBadge.isVisible = false
            }
        } ?: run {
            safeBinding.layoutRatingBadge.isVisible = false
        }

        val subtext = listOfNotNull(candidate.releaseYear, candidate.runtimeOrEpisodes).joinToString(" • ")
        safeBinding.tvSubtitle.text = subtext
        safeBinding.tvSubtitle.isVisible = subtext.isNotBlank()

        safeBinding.ivPoster.load(candidate.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder)
            error(R.drawable.ic_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RandomizerBottomSheet"
        fun newInstance() = RandomizerBottomSheet()
    }
}
