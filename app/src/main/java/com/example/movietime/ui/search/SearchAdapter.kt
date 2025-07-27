package com.example.movietime.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.data.model.ApiMediaItem
import com.example.movietime.databinding.ItemSearchResultBinding
import com.example.movietime.util.loadImage

class SearchAdapter(private val onItemClick: (ApiMediaItem) -> Unit) :
    ListAdapter<ApiMediaItem, SearchAdapter.SearchViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    class SearchViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ApiMediaItem) {
            binding.tvTitle.text = item.universalTitle
            binding.ivPoster.loadImage(item.posterPath)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ApiMediaItem>() {
            override fun areItemsTheSame(oldItem: ApiMediaItem, newItem: ApiMediaItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ApiMediaItem, newItem: ApiMediaItem) =
                oldItem == newItem
        }
    }
}