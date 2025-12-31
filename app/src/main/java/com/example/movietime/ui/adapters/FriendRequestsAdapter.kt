package com.example.movietime.ui.adapters

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
import com.google.android.material.button.MaterialButton
import com.example.movietime.R
import com.example.movietime.data.firebase.FriendRequest
import com.example.movietime.data.firebase.FirebaseUser

data class FriendRequestWithUser(
    val request: FriendRequest,
    val fromUser: FirebaseUser
)

class FriendRequestsAdapter(
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : ListAdapter<FriendRequestWithUser, FriendRequestsAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: MaterialButton = itemView.findViewById(R.id.btnDecline)

        fun bind(item: FriendRequestWithUser) {
            txtName.text = item.fromUser.displayName
            txtUsername.text = "@${item.fromUser.username}"

            if (!item.fromUser.photoUrl.isNullOrEmpty()) {
                imgAvatar.load(item.fromUser.photoUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person_24)
            }

            btnAccept.setOnClickListener { onAccept(item.request) }
            btnDecline.setOnClickListener { onDecline(item.request) }
        }
    }

    private class RequestDiffCallback : DiffUtil.ItemCallback<FriendRequestWithUser>() {
        override fun areItemsTheSame(oldItem: FriendRequestWithUser, newItem: FriendRequestWithUser): Boolean {
            return oldItem.request.id == newItem.request.id
        }

        override fun areContentsTheSame(oldItem: FriendRequestWithUser, newItem: FriendRequestWithUser): Boolean {
            return oldItem == newItem
        }
    }
}
