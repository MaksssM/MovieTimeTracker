package com.example.movietime.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.data.db.WatchedItem
import com.example.movietime.databinding.ItemWatchedBinding
import com.example.movietime.util.formatMinutesToHoursAndMinutes
import com.example.movietime.util.loadImage

class WatchedAdapter : ListAdapter<WatchedItem, WatchedAdapter.WatchedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedViewHolder {
        val binding = ItemWatchedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WatchedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchedViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    class WatchedViewHolder(private val binding: ItemWatchedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WatchedItem) {
            binding.tvTitle.text = item.title
            binding.tvRuntime.text = formatMinutesToHoursAndMinutes(item.runtimeInMinutes)
            binding.ivPoster.loadImage(item.posterPath)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WatchedItem>() {
            override fun areItemsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WatchedItem, newItem: WatchedItem) =
                oldItem == newItem
        }
    }
}