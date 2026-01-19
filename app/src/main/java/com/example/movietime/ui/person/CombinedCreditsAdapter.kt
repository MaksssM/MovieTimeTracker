package com.example.movietime.ui.person

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.databinding.ItemCastBinding // Reuse ItemCastBinding as it has similar layout (poster + title + subtitle)
import com.example.movietime.data.model.CombinedCastCredit
import com.example.movietime.data.model.CombinedCrewCredit

// Consolidated model for the adapter
data class CreditItem(
    val id: Int,
    val title: String,
    val description: String?, // Character or Job
    val posterPath: String?,
    val mediaType: String,
    val year: String?
)

class CombinedCreditsAdapter(
    private val onItemClick: (Int, String) -> Unit // id, mediaType
) : ListAdapter<CreditItem, CombinedCreditsAdapter.CreditViewHolder>(CreditDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val binding = ItemCastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Reuse item_cast layout which works well for vertical poster + 2 lines of text
        return CreditViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CreditViewHolder(
        private val binding: ItemCastBinding,
        private val onItemClick: (Int, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CreditItem) {
            binding.tvName.text = item.title
            binding.tvCharacter.text = item.description ?: ""
            
            // Append year if available
            if (!item.year.isNullOrEmpty()) {
                binding.tvCharacter.text = if (item.description.isNullOrEmpty()) {
                    item.year
                } else {
                    "${item.description} (${item.year})"
                }
            }

            val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
            binding.ivProfile.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder) // Ensure you have a placeholder
                error(R.drawable.ic_placeholder)
            }

            itemView.setOnClickListener {
                onItemClick(item.id, item.mediaType)
            }
        }
    }

    companion object {
        private val CreditDiffCallback = object : DiffUtil.ItemCallback<CreditItem>() {
            override fun areItemsTheSame(oldItem: CreditItem, newItem: CreditItem): Boolean {
                return oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType
            }

            override fun areContentsTheSame(oldItem: CreditItem, newItem: CreditItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
