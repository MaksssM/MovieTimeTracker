package com.example.movietime.ui.today.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.data.model.PersonalTip
import com.example.movietime.data.model.TipType
import com.example.movietime.databinding.ItemPersonalTipBinding

class PersonalTipAdapter(
    private val onItemClick: (PersonalTip) -> Unit
) : ListAdapter<PersonalTip, PersonalTipAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonalTipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPersonalTipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.btnAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: PersonalTip) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description

            // Set icon and background based on tip type
            val (iconRes, bgRes) = when (item.type) {
                TipType.CONTINUE_WATCHING -> Pair(R.drawable.ic_play_circle_filled, R.drawable.bg_icon_glow_primary)
                TipType.NEW_SEASON_AVAILABLE -> Pair(R.drawable.ic_new_releases_24, R.drawable.bg_icon_glow_accent)
                TipType.SIMILAR_CONTENT -> Pair(R.drawable.ic_lightbulb_24, R.drawable.bg_icon_glow_success)
                TipType.MILESTONE_REACHED -> Pair(R.drawable.ic_trophy_24, R.drawable.bg_icon_glow_warning)
                TipType.UPCOMING_REMINDER -> Pair(R.drawable.ic_calendar_24, R.drawable.bg_icon_glow_info)
                TipType.REWATCH_SUGGESTION -> Pair(R.drawable.ic_replay_24, R.drawable.bg_icon_glow_primary)
            }

            binding.ivIcon.setImageResource(item.iconResId ?: iconRes)
            binding.iconContainer.setBackgroundResource(bgRes)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PersonalTip>() {
        override fun areItemsTheSame(oldItem: PersonalTip, newItem: PersonalTip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PersonalTip, newItem: PersonalTip): Boolean {
            return oldItem == newItem
        }
    }
}
