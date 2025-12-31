package com.example.movietime.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movietime.R
import com.example.movietime.ui.adapters.SocialActivityAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsFeedFragment : Fragment() {

    private val viewModel: FriendsViewModel by activityViewModels()
    
    private lateinit var recyclerFeed: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var activityAdapter: SocialActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerFeed = view.findViewById(R.id.recyclerFeed)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        activityAdapter = SocialActivityAdapter(
            onLikeClick = { activity ->
                viewModel.toggleLike(activity)
            },
            onCommentClick = { activity ->
                // TODO: Open comments dialog
            },
            onRecommendClick = { activity ->
                // TODO: Open recommend dialog
            },
            onActivityClick = { activity ->
                // TODO: Open movie/TV details
            }
        )
        
        recyclerFeed.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = activityAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.friendsActivities.collectLatest { activities ->
                activityAdapter.submitList(activities)
                layoutEmpty.isVisible = activities.isEmpty()
                recyclerFeed.isVisible = activities.isNotEmpty()
            }
        }
    }

    companion object {
        fun newInstance() = FriendsFeedFragment()
    }
}
