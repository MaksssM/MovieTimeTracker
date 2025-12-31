package com.example.movietime.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.ui.adapters.FriendRequestsAdapter
import com.example.movietime.ui.adapters.FriendRecommendationsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsRequestsFragment : Fragment() {

    private val viewModel: FriendsViewModel by activityViewModels()
    
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var titleIncoming: TextView
    private lateinit var recyclerIncoming: RecyclerView
    private lateinit var titleRecommendations: TextView
    private lateinit var recyclerRecommendations: RecyclerView
    
    private lateinit var requestsAdapter: FriendRequestsAdapter
    private lateinit var recommendationsAdapter: FriendRecommendationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends_requests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        titleIncoming = view.findViewById(R.id.titleIncoming)
        recyclerIncoming = view.findViewById(R.id.recyclerIncoming)
        titleRecommendations = view.findViewById(R.id.titleRecommendations)
        recyclerRecommendations = view.findViewById(R.id.recyclerRecommendations)
        
        setupRecyclerViews()
        observeData()
    }

    private fun setupRecyclerViews() {
        requestsAdapter = FriendRequestsAdapter(
            onAccept = { request ->
                viewModel.acceptFriendRequest(request.id)
            },
            onDecline = { request ->
                viewModel.declineFriendRequest(request.id)
            }
        )
        
        recyclerIncoming.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = requestsAdapter
            isNestedScrollingEnabled = false
        }
        
        recommendationsAdapter = FriendRecommendationsAdapter(
            onDetailsClick = { recommendation ->
                // TODO: Open movie/TV details
            },
            onMarkReadClick = { recommendation ->
                viewModel.markRecommendationAsRead(recommendation.id)
            }
        )
        
        recyclerRecommendations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendationsAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.friendRequests.collectLatest { requests ->
                requestsAdapter.submitList(requests)
                titleIncoming.isVisible = requests.isNotEmpty()
                recyclerIncoming.isVisible = requests.isNotEmpty()
                updateEmptyState()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendations.collectLatest { recommendations ->
                recommendationsAdapter.submitList(recommendations)
                titleRecommendations.isVisible = recommendations.isNotEmpty()
                recyclerRecommendations.isVisible = recommendations.isNotEmpty()
                updateEmptyState()
            }
        }
    }
    
    private fun updateEmptyState() {
        val hasRequests = requestsAdapter.currentList.isNotEmpty()
        val hasRecommendations = recommendationsAdapter.currentList.isNotEmpty()
        layoutEmpty.isVisible = !hasRequests && !hasRecommendations
    }

    companion object {
        fun newInstance() = FriendsRequestsFragment()
    }
}
