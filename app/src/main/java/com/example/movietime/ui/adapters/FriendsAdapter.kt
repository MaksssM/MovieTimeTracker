package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.movietime.R
import com.example.movietime.data.firebase.FirebaseUser

class FriendsAdapter(
    private val onFriendClick: (FirebaseUser) -> Unit,
    private val onMoreClick: (FirebaseUser, View) -> Unit
) : ListAdapter<FirebaseUser, FriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val txtStats: TextView = itemView.findViewById(R.id.txtStats)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(user: FirebaseUser) {
            txtName.text = user.displayName
            txtUsername.text = "@${user.username}"
            val totalWatched = user.totalWatchedMovies + user.totalWatchedTvShows
            txtStats.text = itemView.context.getString(R.string.watched_count_format, totalWatched)

            if (!user.photoUrl.isNullOrEmpty()) {
                imgAvatar.load(user.photoUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person_24)
            }

            itemView.setOnClickListener { onFriendClick(user) }
            btnMore.setOnClickListener { onMoreClick(user, it) }
        }
    }

    private class FriendDiffCallback : DiffUtil.ItemCallback<FirebaseUser>() {
        override fun areItemsTheSame(oldItem: FirebaseUser, newItem: FirebaseUser): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FirebaseUser, newItem: FirebaseUser): Boolean {
            return oldItem == newItem
        }
    }
}
