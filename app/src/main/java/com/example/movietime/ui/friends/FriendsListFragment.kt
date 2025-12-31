package com.example.movietime.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.data.firebase.FirebaseUser
import com.example.movietime.ui.adapters.FriendsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsListFragment : Fragment() {

    private val viewModel: FriendsViewModel by activityViewModels()
    
    private lateinit var recyclerFriends: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var friendsAdapter: FriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerFriends = view.findViewById(R.id.recyclerFriends)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(
            onFriendClick = { user ->
                // TODO: Open friend profile
            },
            onMoreClick = { user, anchorView ->
                showFriendMenu(user, anchorView)
            }
        )
        
        recyclerFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.friends.collectLatest { friends ->
                friendsAdapter.submitList(friends)
                layoutEmpty.isVisible = friends.isEmpty()
                recyclerFriends.isVisible = friends.isNotEmpty()
            }
        }
    }

    private fun showFriendMenu(user: FirebaseUser, anchorView: View) {
        PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_friend_options, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_remove_friend -> {
                        viewModel.removeFriend(user.id)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    companion object {
        fun newInstance() = FriendsListFragment()
    }
}
