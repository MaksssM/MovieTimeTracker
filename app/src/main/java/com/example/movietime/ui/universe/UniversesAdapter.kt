package com.example.movietime.ui.universe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.data.repository.UniverseWithProgress
import com.example.movietime.databinding.ItemUniverseBinding

class UniversesAdapter(
    private val onClick: (UniverseWithProgress) -> Unit
) : ListAdapter<UniverseWithProgress, UniversesAdapter.VH>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UniverseWithProgress>() {
            override fun areItemsTheSame(a: UniverseWithProgress, b: UniverseWithProgress) =
                a.universe.id == b.universe.id
            override fun areContentsTheSame(a: UniverseWithProgress, b: UniverseWithProgress) =
                a == b
        }
    }

    inner class VH(private val b: ItemUniverseBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: UniverseWithProgress) {
            b.root.setOnClickListener { onClick(item) }
            b.tvUniverseEmoji.text = item.universe.logoEmoji
            b.tvUniverseName.text = item.universe.name
            b.tvUniverseDescription.text = item.universe.description ?: ""
            b.tvUniverseProgress.text = item.progressText
            b.tvSagaCount.text = b.root.context.resources.getQuantityString(
                com.example.movietime.R.plurals.sagas_count,
                item.sagaCount,
                item.sagaCount
            )
            b.progressUniverse.max = if (item.totalMovies > 0) item.totalMovies else 1
            b.progressUniverse.progress = item.watchedMovies

            // Accent color for the header strip and progress
            val accentHex = item.universe.accentColorHex
            if (!accentHex.isNullOrBlank()) {
                try {
                    val color = Color.parseColor(accentHex)
                    b.headerStrip.setBackgroundColor(color)
                    b.progressUniverse.setIndicatorColor(color)
                } catch (_: Exception) { /* ignore invalid color */ }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemUniverseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
