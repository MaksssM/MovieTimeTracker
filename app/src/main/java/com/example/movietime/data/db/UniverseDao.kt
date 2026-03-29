package com.example.movietime.data.db

import androidx.room.*

@Dao
interface UniverseDao {

    // ── Universes ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM cinematic_universes ORDER BY name ASC")
    suspend fun getAllUniverses(): List<CinematicUniverse>

    @Query("SELECT COUNT(*) FROM cinematic_universes WHERE isSeeded = 1")
    suspend fun getSeededCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUniverses(universes: List<CinematicUniverse>)

    // ── Sagas ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM franchise_sagas WHERE universeId = :universeId ORDER BY displayOrder ASC")
    suspend fun getSagasForUniverse(universeId: Long): List<FranchiseSaga>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSagas(sagas: List<FranchiseSaga>)

    // ── Entries ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM franchise_entries WHERE sagaId = :sagaId ORDER BY displayOrder ASC")
    suspend fun getEntriesForSaga(sagaId: Long): List<FranchiseEntry>

    @Query("""
        SELECT * FROM franchise_entries 
        WHERE universeId = :universeId AND sagaId IS NULL 
        ORDER BY displayOrder ASC
    """)
    suspend fun getUncategorizedEntries(universeId: Long): List<FranchiseEntry>

    @Query("SELECT * FROM franchise_entries WHERE universeId = :universeId ORDER BY displayOrder ASC")
    suspend fun getAllEntriesForUniverse(universeId: Long): List<FranchiseEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<FranchiseEntry>)

    // ── Progress ─────────────────────────────────────────────────────────────

    /**
     * Returns watched movie IDs that appear in a comma-separated list.
     * Used to calculate progress per entry.
     */
    @Query("""
        SELECT COUNT(*) FROM watched_items
        WHERE id IN (:ids) AND mediaType = 'movie'
    """)
    suspend fun countWatchedMoviesIn(ids: List<Int>): Int

    @Query("""
        SELECT COUNT(*) FROM watched_items
        WHERE id = :mediaId AND mediaType = :mediaType
    """)
    suspend fun isWatched(mediaId: Int, mediaType: String): Int
}
