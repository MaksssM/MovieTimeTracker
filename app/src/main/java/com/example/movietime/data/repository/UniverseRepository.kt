package com.example.movietime.data.repository

import com.example.movietime.data.db.CinematicUniverse
import com.example.movietime.data.db.FranchiseEntry
import com.example.movietime.data.db.FranchiseSaga
import com.example.movietime.data.db.UniverseDao
import javax.inject.Inject
import javax.inject.Singleton

// ─── Data models ─────────────────────────────────────────────────────────────

data class EntryProgress(
    val entry: FranchiseEntry,
    val watched: Int,
    val total: Int
) {
    val isComplete: Boolean get() = total > 0 && watched >= total
    val progressText: String get() = "$watched / $total"
}

data class SagaWithEntries(
    val saga: FranchiseSaga,
    val entries: List<EntryProgress>,
    val watchedCount: Int,
    val totalCount: Int
) {
    val progressPercent: Int get() = if (totalCount > 0) (watchedCount * 100 / totalCount) else 0
}

data class UniverseWithProgress(
    val universe: CinematicUniverse,
    val sagaCount: Int,
    val watchedMovies: Int,
    val totalMovies: Int
) {
    val progressPercent: Int get() = if (totalMovies > 0) (watchedMovies * 100 / totalMovies) else 0
    val progressText: String get() = "$watchedMovies / $totalMovies"
}

// ─── Repository ──────────────────────────────────────────────────────────────

@Singleton
class UniverseRepository @Inject constructor(
    private val dao: UniverseDao
) {

    /**
     * Inserts seed data if not yet seeded.
     * Call once on app start (e.g. from ViewModel init).
     */
    suspend fun ensureSeedData() {
        if (dao.getSeededCount() == 0) {
            dao.insertUniverses(FranchiseSeedData.universes)
            dao.insertSagas(FranchiseSeedData.sagas)
            dao.insertEntries(FranchiseSeedData.entries)
        }
    }

    /** Returns all universes with aggregate progress. */
    suspend fun getUniversesWithProgress(): List<UniverseWithProgress> {
        val universes = dao.getAllUniverses()
        return universes.map { universe ->
            val entries = dao.getAllEntriesForUniverse(universe.id)
            val sagas = dao.getSagasForUniverse(universe.id)
            val allIds = entries.flatMap { it.movieIds.splitIds() }
            val watched = if (allIds.isNotEmpty()) dao.countWatchedMoviesIn(allIds) else 0
            val total = allIds.size
            UniverseWithProgress(
                universe = universe,
                sagaCount = sagas.size,
                watchedMovies = watched,
                totalMovies = total
            )
        }
    }

    /** Returns sagas for a universe, each with its entries and progress. */
    suspend fun getSagasWithEntries(universeId: Long): List<SagaWithEntries> {
        val sagas = dao.getSagasForUniverse(universeId)
        return sagas.map { saga ->
            val entries = dao.getEntriesForSaga(saga.id)
            val entryProgresses = entries.map { entry -> buildEntryProgress(entry) }
            SagaWithEntries(
                saga = saga,
                entries = entryProgresses,
                watchedCount = entryProgresses.sumOf { it.watched },
                totalCount = entryProgresses.sumOf { it.total }
            )
        }
    }

    /** Returns entries not assigned to any saga (RELATED, SPIN_OFF, uncategorized). */
    suspend fun getUncategorizedEntries(universeId: Long): List<EntryProgress> {
        return dao.getUncategorizedEntries(universeId).map { buildEntryProgress(it) }
    }

    // ─── Private helpers ─────────────────────────────────────────────

    private suspend fun buildEntryProgress(entry: FranchiseEntry): EntryProgress {
        val ids = entry.movieIds.splitIds()
        val watched = if (ids.isNotEmpty()) dao.countWatchedMoviesIn(ids) else 0
        return EntryProgress(entry = entry, watched = watched, total = entry.totalCount)
    }

    private fun String.splitIds(): List<Int> =
        split(",").mapNotNull { it.trim().toIntOrNull() }
}
