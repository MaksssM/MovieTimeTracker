package com.example.movietime.ui.friends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.firebase.*
import com.example.movietime.ui.adapters.FriendRequestWithUser
import com.example.movietime.ui.adapters.RecommendationWithUser
import com.example.movietime.ui.adapters.SocialActivityItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    // Auth state
    private val _isLoggedIn = MutableStateFlow(firebaseRepository.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    // Friends
    private val _friends = MutableStateFlow<List<FirebaseUser>>(emptyList())
    val friends: StateFlow<List<FirebaseUser>> = _friends.asStateFlow()
    
    // Friend requests with user info
    private val _friendRequests = MutableStateFlow<List<FriendRequestWithUser>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequestWithUser>> = _friendRequests.asStateFlow()
    
    // Sent requests (user IDs)
    private val _sentRequests = MutableStateFlow<List<String>>(emptyList())
    val sentRequests: StateFlow<List<String>> = _sentRequests.asStateFlow()
    
    // Search
    private val _searchResults = MutableStateFlow<List<FirebaseUser>>(emptyList())
    val searchResults: StateFlow<List<FirebaseUser>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    // Friends activity feed with user info
    private val _friendsActivities = MutableStateFlow<List<SocialActivityItem>>(emptyList())
    val friendsActivities: StateFlow<List<SocialActivityItem>> = _friendsActivities.asStateFlow()
    
    // Liked activities by current user
    private val _likedActivities = MutableStateFlow<Set<String>>(emptySet())
    
    // Recommendations with user info
    private val _recommendations = MutableStateFlow<List<RecommendationWithUser>>(emptyList())
    val recommendations: StateFlow<List<RecommendationWithUser>> = _recommendations.asStateFlow()
    
    // Notifications
    private val _notifications = MutableStateFlow<List<FirebaseNotification>>(emptyList())
    val notifications: StateFlow<List<FirebaseNotification>> = _notifications.asStateFlow()
    
    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Messages
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // User cache for efficiency
    private val userCache = mutableMapOf<String, FirebaseUser>()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            firebaseRepository.observeAuthState().collect { user ->
                _isLoggedIn.value = user != null
                if (user != null) {
                    loadUserData()
                } else {
                    clearData()
                }
            }
        }
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load current user profile
                _currentUser.value = firebaseRepository.getCurrentUserProfile()
                
                // Load friends
                loadFriends()
                
                // Observe friend requests
                observeFriendRequests()
                
                // Observe activities
                observeFriendsActivities()
                
                // Observe recommendations
                observeRecommendations()
                
                // Observe notifications
                observeNotifications()
                
                // Get unread count
                _unreadNotificationsCount.value = firebaseRepository.getUnreadNotificationsCount()
                
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun clearData() {
        _currentUser.value = null
        _friends.value = emptyList()
        _friendRequests.value = emptyList()
        _friendsActivities.value = emptyList()
        _recommendations.value = emptyList()
        _notifications.value = emptyList()
        _sentRequests.value = emptyList()
        _likedActivities.value = emptySet()
        userCache.clear()
    }
    
    fun loadFriends() {
        viewModelScope.launch {
            try {
                _friends.value = firebaseRepository.getFriends()
            } catch (e: Exception) {
                _error.postValue("Помилка завантаження друзів: ${e.message}")
            }
        }
    }
    
    private suspend fun getUserById(userId: String): FirebaseUser? {
        return userCache[userId] ?: firebaseRepository.getUserProfile(userId)?.also {
            userCache[userId] = it
        }
    }
    
    private fun observeFriendRequests() {
        viewModelScope.launch {
            firebaseRepository.observeFriendRequests().collect { requests ->
                val requestsWithUsers = requests.mapNotNull { request ->
                    val user = getUserById(request.fromUserId)
                    if (user != null) {
                        FriendRequestWithUser(request, user)
                    } else null
                }
                _friendRequests.value = requestsWithUsers
            }
        }
    }
    
    private fun observeFriendsActivities() {
        viewModelScope.launch {
            firebaseRepository.observeFriendsActivities().collect { activities ->
                val currentUserId = _currentUser.value?.id ?: ""
                val activityItems = activities.mapNotNull { activity ->
                    val user = getUserById(activity.userId)
                    if (user != null) {
                        val isLiked = _likedActivities.value.contains(activity.id)
                        SocialActivityItem(activity, user, isLiked)
                    } else null
                }
                _friendsActivities.value = activityItems
            }
        }
    }
    
    private fun observeRecommendations() {
        viewModelScope.launch {
            firebaseRepository.observeRecommendations().collect { recs ->
                val recsWithUsers = recs.mapNotNull { rec ->
                    val user = getUserById(rec.fromUserId)
                    if (user != null) {
                        RecommendationWithUser(rec, user)
                    } else null
                }
                _recommendations.value = recsWithUsers
            }
        }
    }
    
    private fun observeNotifications() {
        viewModelScope.launch {
            firebaseRepository.observeNotifications().collect { notifs ->
                _notifications.value = notifs
                _unreadNotificationsCount.value = notifs.count { !it.isRead }
            }
        }
    }
    
    // Search users
    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = firebaseRepository.searchUsers(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _error.postValue("Помилка пошуку: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
    // Friend requests
    fun sendFriendRequest(userId: String, message: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseRepository.sendFriendRequest(userId, message)
            result.onSuccess {
                _message.postValue("Запит на дружбу надіслано!")
                _sentRequests.value = _sentRequests.value + userId
            }.onFailure {
                _error.postValue(it.message)
            }
            _isLoading.value = false
        }
    }
    
    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = firebaseRepository.acceptFriendRequest(requestId)
            result.onSuccess {
                _message.postValue("Запит прийнято!")
                loadFriends()
            }.onFailure {
                _error.postValue(it.message)
            }
        }
    }
    
    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = firebaseRepository.declineFriendRequest(requestId)
            result.onSuccess {
                _message.postValue("Запит відхилено")
            }.onFailure {
                _error.postValue(it.message)
            }
        }
    }
    
    fun removeFriend(userId: String) {
        viewModelScope.launch {
            val result = firebaseRepository.removeFriend(userId)
            result.onSuccess {
                _message.postValue("Друга видалено")
                loadFriends()
            }.onFailure {
                _error.postValue(it.message)
            }
        }
    }
    
    // Activity interactions
    fun likeActivity(activityId: String) {
        viewModelScope.launch {
            firebaseRepository.likeActivity(activityId)
            _likedActivities.value = _likedActivities.value + activityId
        }
    }
    
    fun unlikeActivity(activityId: String) {
        viewModelScope.launch {
            firebaseRepository.unlikeActivity(activityId)
            _likedActivities.value = _likedActivities.value - activityId
        }
    }
    
    fun toggleLike(activity: SharedActivity) {
        val activityId = activity.id
        if (_likedActivities.value.contains(activityId)) {
            unlikeActivity(activityId)
        } else {
            likeActivity(activityId)
        }
    }
    
    fun commentOnActivity(activityId: String, text: String) {
        viewModelScope.launch {
            val result = firebaseRepository.commentOnActivity(activityId, text)
            result.onFailure {
                _error.postValue("Помилка: ${it.message}")
            }
        }
    }
    
    // Recommendations
    fun sendRecommendation(
        toUserId: String,
        contentId: Int,
        contentTitle: String,
        contentPoster: String?,
        mediaType: String,
        message: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseRepository.sendRecommendation(
                toUserId, contentId, contentTitle, contentPoster, mediaType, message
            )
            result.onSuccess {
                _message.postValue("Рекомендацію надіслано!")
            }.onFailure {
                _error.postValue(it.message)
            }
            _isLoading.value = false
        }
    }
    
    fun markRecommendationAsRead(recommendationId: String) {
        viewModelScope.launch {
            firebaseRepository.markRecommendationAsRead(recommendationId)
        }
    }
    
    // Notifications
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            firebaseRepository.markNotificationAsRead(notificationId)
        }
    }
    
    // Profile
    fun updateProfile(displayName: String, bio: String, isPublic: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseRepository.updateUserProfile(
                mapOf(
                    "displayName" to displayName,
                    "bio" to bio,
                    "isPublic" to isPublic
                )
            )
            result.onSuccess {
                _message.postValue("Профіль оновлено!")
                _currentUser.value = firebaseRepository.getCurrentUserProfile()
            }.onFailure {
                _error.postValue(it.message)
            }
            _isLoading.value = false
        }
    }
    
    // Auth
    fun signOut() {
        firebaseRepository.signOut()
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}
