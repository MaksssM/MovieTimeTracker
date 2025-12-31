package com.example.movietime.ui.search.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            cardView.isChecked = selected
            cardView.strokeWidth = if (selected) 4 else 0
            
            cardView.setOnClickListener {
                onGenreClick(genre)
                // Animate selection change
                cardView.isChecked = isSelected(genre)
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
