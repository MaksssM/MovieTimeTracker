package com.example.movietime.ui.universe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.data.repository.EntryProgress
import com.example.movietime.data.repository.SagaWithEntries
import com.example.movietime.databinding.ItemFranchiseEntryBinding
import com.example.movietime.databinding.ItemSagaHeaderBinding

// Sealed ADT for adapter items
sealed class UniverseDetailItem {
    data class SagaHeader(val saga: SagaWithEntries) : UniverseDetailItem()
    data class EntryItem(val entry: EntryProgress) : UniverseDetailItem()
    data class UncategorizedHeader(val label: String) : UniverseDetailItem()
}

class UniverseDetailAdapter(
    private val accentColorHex: String?,
    private val onEntryClick: (EntryProgress) -> Unit
) : ListAdapter<UniverseDetailItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val TYPE_SAGA_HEADER = 0
        private const val TYPE_ENTRY = 1
        private const val TYPE_UNCATEGORIZED_HEADER = 2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UniverseDetailItem>() {
            override fun areItemsTheSame(a: UniverseDetailItem, b: UniverseDetailItem) =
                when {
                    a is UniverseDetailItem.SagaHeader && b is UniverseDetailItem.SagaHeader ->
                        a.saga.saga.id == b.saga.saga.id
                    a is UniverseDetailItem.EntryItem && b is UniverseDetailItem.EntryItem ->
                        a.entry.entry.id == b.entry.entry.id
                    a is UniverseDetailItem.UncategorizedHeader && b is UniverseDetailItem.UncategorizedHeader ->
                        a.label == b.label
                    else -> false
                }

            override fun areContentsTheSame(a: UniverseDetailItem, b: UniverseDetailItem) = a == b
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is UniverseDetailItem.SagaHeader -> TYPE_SAGA_HEADER
        is UniverseDetailItem.EntryItem -> TYPE_ENTRY
        is UniverseDetailItem.UncategorizedHeader -> TYPE_UNCATEGORIZED_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SAGA_HEADER -> SagaHeaderVH(ItemSagaHeaderBinding.inflate(inflater, parent, false))
            TYPE_UNCATEGORIZED_HEADER -> SagaHeaderVH(ItemSagaHeaderBinding.inflate(inflater, parent, false))
            else -> EntryVH(ItemFranchiseEntryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val accentColor = try {
            if (!accentColorHex.isNullOrBlank()) Color.parseColor(accentColorHex) else null
        } catch (_: Exception) { null }

        when (val item = getItem(position)) {
            is UniverseDetailItem.SagaHeader -> (holder as SagaHeaderVH).bind(item.saga, accentColor)
            is UniverseDetailItem.EntryItem -> (holder as EntryVH).bind(item.entry, accentColor) {
                onEntryClick(item.entry)
            }
            is UniverseDetailItem.UncategorizedHeader -> (holder as SagaHeaderVH).bindUncategorized(item.label)
        }
    }

    // ─── ViewHolders ──────────────────────────────────────────────────

    inner class SagaHeaderVH(private val b: ItemSagaHeaderBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(saga: SagaWithEntries, accentColor: Int?) {
            b.tvSagaName.text = saga.saga.name
            b.tvSagaYearRange.text = saga.saga.yearRange ?: ""
            b.chipSagaProgress.text = "${saga.watchedCount} / ${saga.totalCount}"
            b.progressSaga.max = if (saga.totalCount > 0) saga.totalCount else 1
            b.progressSaga.progress = saga.watchedCount
            accentColor?.let {
                b.sagaColorBar.setBackgroundColor(it)
                b.progressSaga.setIndicatorColor(it)
            }
        }

        fun bindUncategorized(label: String) {
            b.tvSagaName.text = label
            b.tvSagaYearRange.text = ""
            b.chipSagaProgress.text = ""
            b.progressSaga.progress = 0
        }
    }

    inner class EntryVH(private val b: ItemFranchiseEntryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(ep: EntryProgress, accentColor: Int?, onClick: () -> Unit) {
            b.root.setOnClickListener { onClick() }
            b.tvEntryName.text = ep.entry.name

            // Progress circle
            b.progressEntry.max = if (ep.total > 0) ep.total else 1
            b.progressEntry.progress = ep.watched
            accentColor?.let { b.progressEntry.setIndicatorColor(it) }

            // Progress text
            b.tvEntryProgressText.text = if (ep.total == 1) {
                if (ep.watched >= 1) "✓" else "—"
            } else {
                "${ep.watched}/${ep.total}"
            }

            // Note
            val note = ep.entry.note
            if (!note.isNullOrBlank()) {
                b.tvEntryNote.text = note
                b.tvEntryNote.visibility = android.view.View.VISIBLE
            } else {
                b.tvEntryNote.visibility = android.view.View.GONE
            }

            // Relationship badge
            val rel = ep.entry.relationshipType
            if (rel == "SPIN_OFF" || rel == "RELATED") {
                b.chipRelType.visibility = android.view.View.VISIBLE
                b.chipRelType.text = if (rel == "SPIN_OFF") "Спін-офф" else "Пов'язане"
            } else {
                b.chipRelType.visibility = android.view.View.GONE
            }
        }
    }
}
