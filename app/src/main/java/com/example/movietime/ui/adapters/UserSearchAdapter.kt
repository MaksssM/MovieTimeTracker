package com.example.movietime.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.example.movietime.R
import com.example.movietime.data.firebase.FirebaseUser

enum class UserSearchState {
    CAN_ADD,
    REQUEST_SENT,
    ALREADY_FRIENDS
}

data class UserSearchItem(
    val user: FirebaseUser,
    val state: UserSearchState
)

class UserSearchAdapter(
    private val onAddFriend: (FirebaseUser) -> Unit
) : ListAdapter<UserSearchItem, UserSearchAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val btnAddFriend: MaterialButton = itemView.findViewById(R.id.btnAddFriend)
        private val txtRequestSent: TextView = itemView.findViewById(R.id.txtRequestSent)
        private val imgAlreadyFriends: ImageView = itemView.findViewById(R.id.imgAlreadyFriends)

        fun bind(item: UserSearchItem) {
            val user = item.user

            txtName.text = user.displayName
            txtUsername.text = "@${user.username}"

            if (!user.photoUrl.isNullOrEmpty()) {
                imgAvatar.load(user.photoUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person_24)
            }

            // Show appropriate state
            btnAddFriend.isVisible = item.state == UserSearchState.CAN_ADD
            txtRequestSent.isVisible = item.state == UserSearchState.REQUEST_SENT
            imgAlreadyFriends.isVisible = item.state == UserSearchState.ALREADY_FRIENDS

            btnAddFriend.setOnClickListener { onAddFriend(user) }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<UserSearchItem>() {
        override fun areItemsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem == newItem
        }
    }
}
