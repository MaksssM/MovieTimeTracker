package com.example.movietime.ui.friends

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.movietime.R
import com.example.movietime.ui.adapters.UserSearchAdapter
import com.example.movietime.ui.adapters.UserSearchItem
import com.example.movietime.ui.adapters.UserSearchState
import com.example.movietime.ui.auth.AuthActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class FriendsActivity : AppCompatActivity() {

    private val viewModel: FriendsViewModel by viewModels()
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var layoutNotLoggedIn: LinearLayout
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fabSearch: FloatingActionButton
    private lateinit var loadingOverlay: FrameLayout

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.activity_open_enter, R.anim.smooth_fade_out)

        setContentView(R.layout.activity_friends)
        
        initViews()
        setupToolbar()
        setupViewPager()
        setupClickListeners()
        observeData()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        layoutNotLoggedIn = findViewById(R.id.layoutNotLoggedIn)
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn)
        btnLogin = findViewById(R.id.btnLogin)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        fabSearch = findViewById(R.id.fabSearch)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }
    
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupViewPager() {
        viewPager.adapter = FriendsPagerAdapter(this)
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.friends_tab)
                1 -> getString(R.string.feed_tab)
                2 -> getString(R.string.requests_tab)
                else -> ""
            }
        }.attach()
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }
        
        fabSearch.setOnClickListener {
            showSearchDialog()
        }
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            viewModel.isLoggedIn.collectLatest { isLoggedIn ->
                layoutNotLoggedIn.isVisible = !isLoggedIn
                layoutLoggedIn.isVisible = isLoggedIn
                fabSearch.isVisible = isLoggedIn
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                loadingOverlay.isVisible = isLoading
            }
        }
    }
    
    private fun showSearchDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_search_users, null)
        
        val editSearch = dialogView.findViewById<EditText>(R.id.editSearch)
        val progressSearch = dialogView.findViewById<CircularProgressIndicator>(R.id.progressSearch)
        val recyclerResults = dialogView.findViewById<RecyclerView>(R.id.recyclerResults)
        val layoutEmpty = dialogView.findViewById<LinearLayout>(R.id.layoutEmpty)
        val txtEmptyMessage = dialogView.findViewById<TextView>(R.id.txtEmptyMessage)
        
        val searchAdapter = UserSearchAdapter { user ->
            viewModel.sendFriendRequest(user.id)
        }
        
        recyclerResults.apply {
            layoutManager = LinearLayoutManager(this@FriendsActivity)
            adapter = searchAdapter
        }
        
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()
        
        var searchJob: Job? = null
        
        editSearch.doAfterTextChanged { text ->
            searchJob?.cancel()
            val query = text?.toString()?.trim() ?: ""
            
            if (query.length < 2) {
                layoutEmpty.isVisible = true
                txtEmptyMessage.text = getString(R.string.search_username_hint)
                recyclerResults.isVisible = false
                progressSearch.isVisible = false
                return@doAfterTextChanged
            }
            
            searchJob = lifecycleScope.launch {
                progressSearch.isVisible = true
                delay(300) // Debounce
                
                viewModel.searchUsers(query)
                
                viewModel.searchResults.collectLatest { results ->
                    progressSearch.isVisible = false
                    
                    if (results.isEmpty()) {
                        layoutEmpty.isVisible = true
                        txtEmptyMessage.text = getString(R.string.no_users_found)
                        recyclerResults.isVisible = false
                    } else {
                        layoutEmpty.isVisible = false
                        recyclerResults.isVisible = true
                        
                        val currentUserId = viewModel.currentUser.value?.id ?: ""
                        val friendIds = viewModel.friends.value.map { it.id }.toSet()
                        val pendingRequestIds = viewModel.sentRequests.value.toSet()
                        
                        val items = results
                            .filter { it.id != currentUserId }
                            .map { user ->
                                val state = when {
                                    friendIds.contains(user.id) -> UserSearchState.ALREADY_FRIENDS
                                    pendingRequestIds.contains(user.id) -> UserSearchState.REQUEST_SENT
                                    else -> UserSearchState.CAN_ADD
                                }
                                UserSearchItem(user, state)
                            }
                        searchAdapter.submitList(items)
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.activity_close_exit)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private inner class FriendsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FriendsListFragment.newInstance()
                1 -> FriendsFeedFragment.newInstance()
                2 -> FriendsRequestsFragment.newInstance()
                else -> FriendsListFragment.newInstance()
            }
        }
    }
}