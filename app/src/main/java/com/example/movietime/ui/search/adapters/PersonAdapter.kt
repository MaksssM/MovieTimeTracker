package com.example.movietime.ui.search.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.movietime.R
import com.example.movietime.data.model.Person
import com.google.android.material.card.MaterialCardView

class PersonAdapter(
    private val onPersonClick: (Person) -> Unit
) : ListAdapter<Person, PersonAdapter.PersonViewHolder>(PersonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardPerson)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPersonPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvPersonName)
        private val tvDepartment: TextView = itemView.findViewById(R.id.tvPersonDepartment)
        private val tvKnownFor: TextView = itemView.findViewById(R.id.tvKnownFor)

        fun bind(person: Person) {
            tvName.text = person.name
            
            // Department
            tvDepartment.text = when (person.knownForDepartment) {
                "Acting" -> "Актор/Актриса"
                "Directing" -> "Режисер"
                "Writing" -> "Сценарист"
                "Production" -> "Продюсер"
                "Camera" -> "Оператор"
                "Sound" -> "Звукорежисер"
                "Art" -> "Художник"
                "Crew" -> "Знімальна група"
                else -> person.knownForDepartment ?: ""
            }
            
            // Known for
            val knownForText = person.knownFor?.mapNotNull { 
                it.title ?: it.name 
            }?.joinToString(", ")
            
            if (!knownForText.isNullOrBlank()) {
                tvKnownFor.visibility = View.VISIBLE
                tvKnownFor.text = knownForText
            } else {
                tvKnownFor.visibility = View.GONE
            }
            
            // Photo
            val photoUrl = person.profilePath?.let { 
                "https://image.tmdb.org/t/p/w185$it" 
            }
            
            ivPhoto.load(photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_person_placeholder)
                error(R.drawable.ic_person_placeholder)
                transformations(CircleCropTransformation())
            }
            
            cardView.setOnClickListener {
                onPersonClick(person)
            }
        }
    }

    class PersonDiffCallback : DiffUtil.ItemCallback<Person>() {
        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem == newItem
        }
    }
}
