package com.example.movietime.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.movietime.data.api.TmdbApi
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.data.db.WatchedItemDao
import com.example.movietime.data.model.MovieResult
import com.example.movietime.data.model.MoviesResponse
import com.example.movietime.data.model.TvShowResult
import com.example.movietime.data.model.TvShowsResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: TmdbApi,
    private val dao: WatchedItemDao,
    private val apiKey: String
) {

    // --- API Methods ---
    suspend fun searchMultiLanguage(query: String): List<Any> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()

        // detect script (simple heuristic): if contains Cyrillic -> prefer Ukrainian/Russian first
        val isCyrillic = query.any { ch -> ch in '\u0400'..'\u04FF' }
        val languages = if (isCyrillic) listOf("uk-UA", "ru-RU", "en-US") else listOf("en-US", "uk-UA", "ru-RU")

        // Normalization: lowercase, remove punctuation and diacritics
        fun normalize(text: String?): String = text?.let {
            val lowered = it.lowercase()
            // remove diacritics
            val normalized = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            normalized.replace(Regex("[^\\p{L}0-9\\s]"), "").trim()
        } ?: ""

        // Simple Cyrillic->Latin transliteration for better cross-language matching / API queries
        fun transliterateCyrToLat(input: String): String {
            val map = mapOf(
                '\u0430' to "a",'\u0431' to "b",'\u0432' to "v",'\u0433' to "h",'\u0491' to "g",'\u0434' to "d",'\u0435' to "e",'\u0454' to "e",
                '\u0436' to "zh",'\u0437' to "z",'\u0438' to "y",'\u0456' to "i",'\u0457' to "i",'\u0439' to "i",'\u043a' to "k",'\u043b' to "l",
                '\u043c' to "m",'\u043d' to "n",'\u043e' to "o",'\u043f' to "p",'\u0440' to "r",'\u0441' to "s",'\u0442' to "t",'\u0443' to "u",
                '\u0444' to "f",'\u0445' to "kh",'\u0446' to "ts",'\u0447' to "ch",'\u0448' to "sh",'\u0449' to "shch",'\u044c' to "",'\u044e' to "yu",'\u044f' to "ya"
            )
            val sb = StringBuilder()
            for (ch in input.lowercase()) {
                val repl = map[ch]
                if (repl != null) sb.append(repl) else sb.append(ch)
            }
            return sb.toString()
        }

        val qNorm = normalize(query)
        val qTrans = transliterateCyrToLat(query)

        // Build query variants to send to TMDB (original + transliteration if different)
        val queryVariants = mutableListOf<String>()
        queryVariants.add(query)
        if (qTrans.isNotBlank() && qTrans != qNorm && qTrans.lowercase() != query.lowercase()) {
            queryVariants.add(qTrans)
        }

        Log.d("AppRepo", "searchMultiLanguage() query='$query' qNorm='$qNorm' qTrans='$qTrans' langs=${languages.joinToString()} variants=${queryVariants.joinToString()}")

        val movieDeferred = mutableListOf<kotlinx.coroutines.Deferred<MoviesResponse>>()
        val tvDeferred = mutableListOf<kotlinx.coroutines.Deferred<TvShowsResponse>>()

        // For each language and each query variant, launch async requests
        for (language in languages) {
            for (q in queryVariants) {
                Log.d("AppRepo", "Launching request: type=movie lang=$language q=$q")
                movieDeferred.add(async {
                    try {
                        val resp = api.searchMovies(apiKey, q, language)
                        Log.d("AppRepo", "Movie response: lang=$language q=$q size=${resp.results.size}")
                        resp
                    } catch (e: Exception) {
                        Log.d("AppRepo", "Movie request failed: lang=$language q=$q error=${e.message}")
                        MoviesResponse(emptyList())
                    }
                })

                Log.d("AppRepo", "Launching request: type=tv lang=$language q=$q")
                tvDeferred.add(async {
                    try {
                        val resp = api.searchTvShows(apiKey, q, language)
                        Log.d("AppRepo", "TV response: lang=$language q=$q size=${resp.results.size}")
                        resp
                    } catch (e: Exception) {
                        Log.d("AppRepo", "TV request failed: lang=$language q=$q error=${e.message}")
                        TvShowsResponse(emptyList())
                    }
                })
            }
        }

        val movieResponses = movieDeferred.awaitAll()
        val tvResponses = tvDeferred.awaitAll()

        val movieItems = movieResponses.flatMap { it.results }
        val tvItems = tvResponses.flatMap { it.results }

        Log.d("AppRepo", "Total items fetched: movies=${movieItems.size} tv=${tvItems.size}")

        // Combine items and deduplicate by (id, mediaType)
        val allItems = (movieItems + tvItems)
        val uniqueMap = linkedMapOf<Pair<Int, String>, MutableList<Any>>()
        allItems.forEach { item ->
            val key = when (item) {
                is MovieResult -> item.id to "movie"
                is TvShowResult -> item.id to "tv"
                else -> 0 to ""
            }
            uniqueMap.getOrPut(key) { mutableListOf() }.add(item)
        }

        // For each unique item, compute a relevance score based on query matches across available titles

        // Levenshtein distance for lightweight fuzzy scoring
        fun levenshtein(a: String, b: String): Int {
            if (a == b) return 0
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                }
            }
            return dp[a.length][b.length]
        }

        data class Scored(val item: Any, val score: Int, val vote: Int)

        val scored = uniqueMap.map { (_, occurrences) ->
            // occurrences contains same item from different language results; pick first as representative
            val item = occurrences.first()
            val titles = when (item) {
                is MovieResult -> listOf(item.title)
                is TvShowResult -> listOf(item.name)
                else -> listOf(null)
            }.map { normalize(it) }

            var score = 0
            var vote = occurrences.size

            // exact match > startsWith > contains
            for (t in titles) {
                if (t.isBlank()) continue
                // exact or transliterated exact match
                if (t == qNorm || t == qTrans) {
                    score += 200
                } else if (t.startsWith(qNorm)) {
                    score += 120
                } else if (t.contains(qNorm)) {
                    score += 60
                } else if (qTrans.isNotBlank() && t.contains(qTrans)) {
                    score += 80
                } else {
                    // small fuzzy match: common prefix length
                    val common = maxOf(t.commonPrefixWith(qNorm).length, t.commonPrefixWith(qTrans).length)
                    score += common * 2
                    // also use levenshtein distance to give small score for close matches
                    val dist = levenshtein(t, qNorm).coerceAtMost(levenshtein(t, qTrans))
                    if (dist <= 3) score += (4 - dist) * 15
                }
            }

            // boost by popularity if available
            val popularityBoost = when (item) {
                is MovieResult -> (item.voteAverage * 10).toInt()
                is TvShowResult -> (item.voteAverage * 10).toInt()
                else -> 0
            }
            score += popularityBoost

            Scored(item, score, vote)
        }

        // Sort by score desc, then by vote (appears in multiple languages), then by id
        val sorted = scored.sortedWith(compareByDescending<Scored> { it.score }
            .thenByDescending { it.vote })

        // Return top 100 items as List<Any>
        val result = sorted.map { it.item }.take(100)
        Log.d("AppRepo", "Returning results count=${result.size}")
        result
    }

    // Details endpoints return single objects
    suspend fun getMovieDetails(movieId: Int, language: String = "en-US"): MovieResult {
        return api.getMovieDetails(movieId, apiKey, language)
    }

    suspend fun getTvShowDetails(tvId: Int, language: String = "en-US"): TvShowResult {
        return api.getTvShowDetails(tvId, apiKey, language)
    }

    suspend fun getPopularMovies(language: String = "en-US"): MoviesResponse {
        return api.getPopularMovies(apiKey, language)
    }

    suspend fun getPopularTvShows(language: String = "en-US"): TvShowsResponse {
        return api.getPopularTvShows(apiKey, language)
    }

    fun getWatchedItems(): LiveData<List<WatchedItem>> {
        return dao.getAll()
    }

    suspend fun addWatchedItem(item: WatchedItem) {
        dao.insert(item)
    }

    suspend fun deleteWatchedItem(item: WatchedItem) {
        dao.deleteById(item.id, item.mediaType)
    }

    // Updated: get watched item by id + mediaType (composite primary key)
    suspend fun getWatchedItemById(id: Int, mediaType: String): WatchedItem? {
        return dao.getById(id, mediaType)
    }
}