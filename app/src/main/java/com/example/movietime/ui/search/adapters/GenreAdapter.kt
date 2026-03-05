package com.example.movietime.ui.search.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.data.model.Genre
import com.google.android.material.card.MaterialCardView

class GenreAdapter(
    private val onGenreClick: (Genre) -> Unit,
    private val isSelected: (Genre) -> Boolean
) : ListAdapter<Genre, GenreAdapter.GenreViewHolder>(GenreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre_chip, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardGenre)
        private val tvGenreName: TextView = itemView.findViewById(R.id.tvGenreName)

        fun bind(genre: Genre) {
            tvGenreName.text = genre.name
            
            val selected = isSelected(genre)
            val context = itemView.context
            cardView.isChecked = selected
            
            if (selected) {
                cardView.strokeWidth = 4
                cardView.strokeColor = ContextCompat.getColor(context, R.color.accent)
                cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_light))
                )
                tvGenreName.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            } else {
                cardView.strokeWidth = 2
                cardView.strokeColor = ContextCompat.getColor(context, R.color.divider)
                cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant))
                )
                tvGenreName.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            }
            
            cardView.setOnClickListener {
                onGenreClick(genre)
                // Re-bind to update visual state
                bind(genre)
            }
        }
    }

    class GenreDiffCallback : DiffUtil.ItemCallback<Genre>() {
        override fun areItemsTheSame(oldItem: Genre, newItem: Genre): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Genre, newItem: Genre): Boolean {
            return oldItem == newItem
        }
    }
}
