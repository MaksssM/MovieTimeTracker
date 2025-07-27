package com.example.movietime.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.data.model.ApiMovie
import com.example.movietime.databinding.ItemSearchResultBinding

class SearchAdapter : ListAdapter<ApiMovie, SearchAdapter.SearchViewHolder>(DiffCallback) {

    var onItemClick: ((ApiMovie) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class SearchViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(item: ApiMovie) {
            binding.tvTitle.text = item.title ?: item.name
            binding.ivPoster.load("https://image.tmdb.org/t/p/w500" + item.posterPath) {
                crossfade(true)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ApiMovie>() {
            override fun areItemsTheSame(oldItem: ApiMovie, newItem: ApiMovie): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ApiMovie, newItem: ApiMovie): Boolean {
                return oldItem == newItem
            }
        }
    }
}