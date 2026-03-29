package com.example.movietime.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R

class RecentSearchChipsAdapter(
    private var items: List<String>,
    private val onChipClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchChipsAdapter.ChipViewHolder>() {

    inner class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChip: TextView = itemView.findViewById(R.id.tvChipText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val query = items[position]
        holder.tvChip.text = query
        holder.itemView.setOnClickListener {
            onChipClick(query)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}
