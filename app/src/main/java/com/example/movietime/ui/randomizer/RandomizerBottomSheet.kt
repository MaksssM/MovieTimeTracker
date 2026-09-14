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
    val runtimeOrEpisodes: String?
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
            loadCandidatesForCurrentSource()
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.chipGroupSource.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                loadCandidatesForCurrentSource()
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

    private fun loadCandidatesForCurrentSource() {
        val safeBinding = _binding ?: return
        val source = when {
            safeBinding.chipRecommendations.isChecked -> CandidateSource.RECOMMENDATIONS
            safeBinding.chipTopRated.isChecked -> CandidateSource.TOP_RATED
            else -> CandidateSource.PLANNED
        }
        val loadVersion = ++candidateLoadVersion

        selectedCandidate = null
        currentCandidates = emptyList()
        safeBinding.layoutMovieResult.isVisible = false
        safeBinding.layoutPlaceholder.isVisible = true
        safeBinding.btnViewDetails.isVisible = false
        safeBinding.btnSpin.isEnabled = false
        safeBinding.btnSpin.text = getString(R.string.loading)

        viewLifecycleOwner.lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) { loadCandidates(source) }
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

    private suspend fun loadCandidates(source: CandidateSource): List<RandomMovieCandidate> = try {
        when (source) {
            CandidateSource.PLANNED -> {
                val planned = repository.getPlannedItemsSync().map { it.toRandomCandidate() }
                if (planned.isEmpty()) {
                    // Fallback to top recommendations if planned list is empty
                    val recs = recommendationService.getPersonalizedRecommendations()
                    (recs.movies.map { it.toRandomCandidate() } + recs.tvShows.map { it.toRandomCandidate() }).shuffled()
                } else {
                    planned
                }
            }

            CandidateSource.RECOMMENDATIONS -> {
                val recommendations = recommendationService.getPersonalizedRecommendations()
                val list = (recommendations.movies.map { it.toRandomCandidate() } +
                    recommendations.tvShows.map { it.toRandomCandidate() }).shuffled()
                list.ifEmpty { loadTopRatedFallback() }
            }

            CandidateSource.TOP_RATED -> {
                loadTopRatedFallback()
            }
        }
    } catch (_: Exception) {
        loadTopRatedFallback()
    }

    private suspend fun loadTopRatedFallback(): List<RandomMovieCandidate> {
        return try {
            val popularMovies = repository.getPopularMovies().results
            val popularTv = repository.getPopularTvShows().results
            val seenIds = repository.getAllSeenItemIds()
                .map { "${it.id}_${it.mediaType}" }
                .toSet()

            val unseenMovies = popularMovies.filter { "${it.id}_movie" !in seenIds }
            val unseenTv = popularTv.filter { "${it.id}_tv" !in seenIds }

            val movies = (unseenMovies.ifEmpty { popularMovies }).map { it.toRandomCandidate() }
            val tvShows = (unseenTv.ifEmpty { popularTv }).map { it.toRandomCandidate() }
            (movies + tvShows).take(40).shuffled()
        } catch (_: Exception) {
            emptyList()
        }
    }

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
        }
    )

    private fun MovieResult.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = title ?: getString(R.string.no_title),
        posterPath = posterPath,
        rating = voteAverage.toDouble(),
        mediaType = "movie",
        releaseYear = releaseDate?.take(4),
        runtimeOrEpisodes = runtime?.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" }
    )

    private fun TvShowResult.toRandomCandidate() = RandomMovieCandidate(
        id = id,
        title = name ?: getString(R.string.no_title),
        posterPath = posterPath,
        rating = voteAverage.toDouble(),
        mediaType = "tv",
        releaseYear = firstAirDate?.take(4),
        runtimeOrEpisodes = numberOfEpisodes?.takeIf { it > 0 }
            ?.let { "$it ${getString(R.string.episodes)}" }
    )

    private fun spinWheel() {
        val safeBinding = _binding ?: return
        if (isSpinning) return

        if (currentCandidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_random_items, Toast.LENGTH_SHORT).show()
            loadCandidatesForCurrentSource()
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
