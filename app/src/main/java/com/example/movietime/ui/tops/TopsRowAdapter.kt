package com.example.movietime.ui.tops

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.movietime.R
import com.example.movietime.databinding.ItemTopCardBinding
import com.example.movietime.databinding.ItemTopRowBinding
import java.util.Locale

class TopsRowAdapter(
    private val onItemClick: (TopItem, android.view.View) -> Unit
) : ListAdapter<TopRow, TopsRowAdapter.RowViewHolder>(RowDiffCallback) {

    companion object {
        private val RowDiffCallback = object : DiffUtil.ItemCallback<TopRow>() {
            override fun areItemsTheSame(oldItem: TopRow, newItem: TopRow): Boolean =
                oldItem.titleRes == newItem.titleRes

            override fun areContentsTheSame(oldItem: TopRow, newItem: TopRow): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemTopRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RowViewHolder(
        private val binding: ItemTopRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val cardAdapter = TopCardAdapter(onItemClick)

        init {
            binding.rvRow.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = cardAdapter
                setHasFixedSize(true)
            }
        }

        fun bind(row: TopRow) {
            binding.tvRowTitle.text = binding.root.context.getString(row.titleRes)
            cardAdapter.submitList(row.items)
        }
    }
}

private class TopCardAdapter(
    private val onItemClick: (TopItem, android.view.View) -> Unit
) : ListAdapter<TopItem, TopCardAdapter.CardViewHolder>(CardDiffCallback) {

    companion object {
        private val CardDiffCallback = object : DiffUtil.ItemCallback<TopItem>() {
            override fun areItemsTheSame(oldItem: TopItem, newItem: TopItem): Boolean =
                oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType

            override fun areContentsTheSame(oldItem: TopItem, newItem: TopItem): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemTopCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(
        private val binding: ItemTopCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopItem) {
            binding.tvTitle.text = item.title
            binding.tvRating.text = String.format(Locale.US, "%.1f", item.rating)
            if (!item.posterPath.isNullOrEmpty()) {
                binding.ivPoster.load("https://image.tmdb.org/t/p/w342${item.posterPath}") {
                    crossfade(true)
                    placeholder(R.color.poster_placeholder_dark)
                    error(R.color.poster_placeholder_dark)
                }
            } else {
                binding.ivPoster.setImageResource(R.color.poster_placeholder_dark)
            }
            binding.root.setOnClickListener { onItemClick(item, binding.ivPoster) }
        }
    }
}
