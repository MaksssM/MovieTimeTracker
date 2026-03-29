package com.example.movietime.ui.universe

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.data.repository.EntryProgress
import com.example.movietime.data.repository.SagaWithEntries
import com.example.movietime.databinding.ActivityUniverseDetailBinding
import com.example.movietime.ui.details.DetailsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UniverseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUniverseDetailBinding
    private val viewModel: UniverseDetailViewModel by viewModels()

    private var adapter: UniverseDetailAdapter? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUniverseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val universeName = intent.getStringExtra(EXTRA_UNIVERSE_NAME) ?: "Кіновсесвіт"
        supportActionBar?.title = universeName

        binding.rvDetail.layoutManager = LinearLayoutManager(this)

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.universe.observe(this) { u ->
            if (u != null) {
                binding.tvHeaderEmoji.text = u.universe.logoEmoji
                binding.tvUniverseName.text = u.universe.name
                binding.progressUniverseDetail.max = if (u.totalMovies > 0) u.totalMovies else 1
                binding.progressUniverseDetail.progress = u.watchedMovies
                binding.tvProgressDetail.text = u.progressText

                val accentHex = u.universe.accentColorHex
                if (!accentHex.isNullOrBlank()) {
                    try {
                        val c = Color.parseColor(accentHex)
                        binding.headerFrame.setBackgroundColor(c)
                    } catch (_: Exception) {}
                }

                // Init adapter with accent color; observe sagas after
                val hex = u.universe.accentColorHex
                adapter = UniverseDetailAdapter(hex) { entryProgress ->
                    navigateToEntry(entryProgress)
                }
                binding.rvDetail.adapter = adapter
            }
        }

        viewModel.sagas.observe(this) { sagas ->
            viewModel.uncategorized.value?.let { uncategorized ->
                submitItems(sagas, uncategorized)
            }
        }

        viewModel.uncategorized.observe(this) { uncategorized ->
            viewModel.sagas.value?.let { sagas ->
                submitItems(sagas, uncategorized)
            }
        }
    }

    private fun submitItems(
        sagas: List<SagaWithEntries>,
        uncategorized: List<EntryProgress>
    ) {
        val items = mutableListOf<UniverseDetailItem>()
        for (saga in sagas) {
            items.add(UniverseDetailItem.SagaHeader(saga))
            saga.entries.forEach { items.add(UniverseDetailItem.EntryItem(it)) }
        }
        if (uncategorized.isNotEmpty()) {
            items.add(UniverseDetailItem.UncategorizedHeader("Пов'язане"))
            uncategorized.forEach { items.add(UniverseDetailItem.EntryItem(it)) }
        }
        adapter?.submitList(items)
    }

    private fun navigateToEntry(ep: EntryProgress) {
        val entry = ep.entry
        val mediaId: Int?
        val mediaType: String?

        when (entry.entryType) {
            "STANDALONE_MOVIE" -> {
                mediaId = entry.tmdbMediaId
                mediaType = "movie"
            }
            "STANDALONE_TV" -> {
                mediaId = entry.tmdbMediaId
                mediaType = "tv"
            }
            "TMDB_COLLECTION" -> {
                // Navigate to collection details
                entry.tmdbCollectionId?.let { collId ->
                    val intent = Intent(this,
                        com.example.movietime.ui.collection.CollectionDetailsActivity::class.java).apply {
                        putExtra("COLLECTION_ID", collId)
                        putExtra("COLLECTION_NAME", entry.name)
                    }
                    startActivity(intent)
                }
                return
            }
            else -> return
        }
        if (mediaId != null && mediaType != null) {
            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtra("ITEM_ID", mediaId)
                putExtra("MEDIA_TYPE", mediaType)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_UNIVERSE_ID = "universe_id"
        const val EXTRA_UNIVERSE_NAME = "universe_name"

        fun start(context: Context, universeId: Long, universeName: String) {
            context.startActivity(Intent(context, UniverseDetailActivity::class.java).apply {
                putExtra(EXTRA_UNIVERSE_ID, universeId)
                putExtra(EXTRA_UNIVERSE_NAME, universeName)
            })
        }
    }
}
