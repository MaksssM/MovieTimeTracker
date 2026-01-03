package com.example.movietime.ui.search.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.data.model.CompanyResult
import com.google.android.material.card.MaterialCardView

class CompanyAdapter(
    private val onCompanyClick: (CompanyResult) -> Unit
) : ListAdapter<CompanyResult, CompanyAdapter.CompanyViewHolder>(CompanyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_company, parent, false)
        return CompanyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompanyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardCompany)
        private val tvName: TextView = itemView.findViewById(R.id.tvCompanyName)
        private val tvCountry: TextView = itemView.findViewById(R.id.tvCompanyCountry)

        fun bind(company: CompanyResult) {
            tvName.text = company.name ?: itemView.context.getString(R.string.no_title)

            if (company.originCountry.isNullOrBlank()) {
                tvCountry.visibility = View.GONE
            } else {
                tvCountry.visibility = View.VISIBLE
                tvCountry.text = company.originCountry
            }

            cardView.setOnClickListener {
                onCompanyClick(company)
            }
        }
    }

    class CompanyDiffCallback : DiffUtil.ItemCallback<CompanyResult>() {
        override fun areItemsTheSame(oldItem: CompanyResult, newItem: CompanyResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CompanyResult, newItem: CompanyResult): Boolean {
            return oldItem == newItem
        }
    }
}
