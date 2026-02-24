package com.example.movietime.ui.collections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.data.repository.CollectionWithCount
import com.example.movietime.databinding.ItemCollectionBinding

class CollectionsAdapter(
    private val onCollectionClick: (CollectionWithCount) -> Unit
) : ListAdapter<CollectionWithCount, CollectionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCollectionClick(getItem(position))
                }
            }
        }

        fun bind(item: CollectionWithCount) {
            with(binding) {
                tvCollectionName.text = item.collection.name
                tvCollectionDescription.text = item.collection.description
                tvCollectionDescription.isVisible = !item.collection.description.isNullOrEmpty()
                
                tvItemCount.text = itemView.context.getString(R.string.items_count, item.itemCount)
                
                // Emoji / Icon
                tvEmoji.text = item.collection.emoji ?: "📂"
                tvEmoji.isVisible = item.previewPosters.isEmpty()
                gridCover.isVisible = item.previewPosters.isNotEmpty()

                // Load posters if available
                if (item.previewPosters.isNotEmpty()) {
                    val posters = item.previewPosters
                    val imageViews = listOf(ivCover1, ivCover2, ivCover3, ivCover4)
                    
                    imageViews.forEachIndexed { index, imageView ->
                        if (index < posters.size) {
                            imageView.isVisible = true
                            imageView.load("https://image.tmdb.org/t/p/w200${posters[index]}") {
                                crossfade(true)
                            }
                        } else {
                            // Hide extra slots if we have fewer than 4 posters, 
                            // OR show a placeholder if we want a full grid.
                            // For a nice collage, usually we just fill what we have.
                            // If we have < 4, we might want to change layout logic, 
                            // but for simplicity let's just leave empty slots transparent 
                            // or use the background color
                            imageView.isVisible = true
                            imageView.setImageDrawable(null)
                        }
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CollectionWithCount>() {
        override fun areItemsTheSame(oldItem: CollectionWithCount, newItem: CollectionWithCount): Boolean {
            return oldItem.collection.id == newItem.collection.id
        }

        override fun areContentsTheSame(oldItem: CollectionWithCount, newItem: CollectionWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
