package com.example.movietime.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Collections
    private val usersCollection = firestore.collection("users")
    private val friendshipsCollection = firestore.collection("friendships")
    private val friendRequestsCollection = firestore.collection("friend_requests")
    private val activitiesCollection = firestore.collection("activities")
    private val recommendationsCollection = firestore.collection("recommendations")
    private val notificationsCollection = firestore.collection("notifications")
    
    companion object {
        private const val TAG = "FirebaseRepository"
    }
    
    // ==================== AUTH ====================
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val currentUserId: String?
        get() = auth.currentUser?.uid
    
    val isLoggedIn: Boolean
        get() = auth.currentUser != null
    
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                // Create or update user profile in Firestore
                createOrUpdateUserProfile(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Create user profile
                val userProfile = com.example.movietime.data.firebase.FirebaseUser(
                    id = user.uid,
                    email = email,
                    displayName = displayName,
                    username = generateUsername(displayName)
                )
                usersCollection.document(user.uid).set(userProfile).await()
                Result.success(user)
            } else {
                Result.failure(Exception("Sign up failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up failed", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    private suspend fun createOrUpdateUserProfile(user: FirebaseUser) {
        val userDoc = usersCollection.document(user.uid).get().await()
        if (!userDoc.exists()) {
            // New user - create profile
            val newUser = com.example.movietime.data.firebase.FirebaseUser(
                id = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "User",
                username = generateUsername(user.displayName ?: "user"),
                photoUrl = user.photoUrl?.toString()
            )
            usersCollection.document(user.uid).set(newUser).await()
        } else {
            // Existing user - update last active
            usersCollection.document(user.uid).update(
                mapOf("lastActive" to com.google.firebase.firestore.FieldValue.serverTimestamp())
            ).await()
        }
    }
    
    private fun generateUsername(displayName: String): String {
        val base = displayName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
        return "${base}_${System.currentTimeMillis() % 10000}"
    }
    
    // ==================== USER PROFILE ====================
    
    suspend fun getCurrentUserProfile(): com.example.movietime.data.firebase.FirebaseUser? {
        val userId = currentUserId ?: return null
        return try {
            usersCollection.document(userId).get().await()
                .toObject(com.example.movietime.data.firebase.FirebaseUser::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile", e)
            null
        }
    }
    
    suspend fun getUserProfile(userId: String): com.example.movietime.data.firebase.FirebaseUser? {
        return try {
            usersCollection.document(userId).get().await()
                .toObject(com.example.movietime.data.firebase.FirebaseUser::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile: $userId", e)
            null
        }
    }
    
    fun observeUserProfile(userId: String): Flow<com.example.movietime.data.firebase.FirebaseUser?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing user profile", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(com.example.movietime.data.firebase.FirebaseUser::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String): List<com.example.movietime.data.firebase.FirebaseUser> {
        if (query.length < 2) return emptyList()
        return try {
            // Search by username or display name (case-insensitive search is limited in Firestore)
            val queryLower = query.lowercase()
            val results = mutableListOf<com.example.movietime.data.firebase.FirebaseUser>()
            
            // Search by username prefix
            val usernameResults = usersCollection
                .whereGreaterThanOrEqualTo("username", queryLower)
                .whereLessThanOrEqualTo("username", queryLower + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .toObjects(com.example.movietime.data.firebase.FirebaseUser::class.java)
            
            results.addAll(usernameResults)
            
            // Filter out current user and private profiles
            results.filter { 
                it.id != currentUserId && it.isPublic 
            }.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search users", e)
            emptyList()
        }
    }
    
    // ==================== FRIENDS ====================
    
    suspend fun sendFriendRequest(toUserId: String, message: String = ""): Result<Unit> {
        val fromUserId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val currentProfile = getCurrentUserProfile() ?: return Result.failure(Exception("Profile not found"))
        
        return try {
            // Check if request already exists
            val existingRequest = friendRequestsCollection
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .get()
                .await()
            
            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Request already sent"))
            }
            
            // Check if already friends
            val existingFriendship = checkFriendship(fromUserId, toUserId)
            if (existingFriendship) {
                return Result.failure(Exception("Already friends"))
            }
            
            val request = FriendRequest(
                fromUserId = fromUserId,
                toUserId = toUserId,
                fromUserName = currentProfile.displayName,
                fromUserPhoto = currentProfile.photoUrl,
                message = message
            )
            
            friendRequestsCollection.add(request).await()
            
            // Send notification
            sendNotification(
                toUserId,
                NotificationType.FRIEND_REQUEST,
                "Новий запит на дружбу",
                "${currentProfile.displayName} хоче додати вас у друзі",
                mapOf("fromUserId" to fromUserId)
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send friend request", e)
            Result.failure(e)
        }
    }
    
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        val currentId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return Result.failure(Exception("Request not found"))
            
            if (request.toUserId != currentId) {
                return Result.failure(Exception("Not your request"))
            }
            
            // Update request status
            friendRequestsCollection.document(requestId).update("status", RequestStatus.ACCEPTED.name).await()
            
            // Create friendship
            val friendship = Friendship(
                user1Id = request.fromUserId,
                user2Id = request.toUserId,
                status = FriendshipStatus.ACCEPTED
            )
            friendshipsCollection.add(friendship).await()
            
            // Update friend counts
            incrementFriendCount(request.fromUserId)
            incrementFriendCount(request.toUserId)
            
            // Send notification
            val currentProfile = getCurrentUserProfile()
            sendNotification(
                request.fromUserId,
                NotificationType.FRIEND_ACCEPTED,
                "Запит прийнято",
                "${currentProfile?.displayName ?: "Користувач"} прийняв ваш запит на дружбу",
                mapOf("userId" to currentId)
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to accept friend request", e)
            Result.failure(e)
        }
    }
    
    suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            friendRequestsCollection.document(requestId).update("status", RequestStatus.DECLINED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decline friend request", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeFriend(friendUserId: String): Result<Unit> {
        val currentId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Find and delete friendship
            val friendships = friendshipsCollection
                .whereIn("user1Id", listOf(currentId, friendUserId))
                .get()
                .await()
            
            for (doc in friendships.documents) {
                val friendship = doc.toObject(Friendship::class.java)
                if (friendship != null && 
                    ((friendship.user1Id == currentId && friendship.user2Id == friendUserId) ||
                     (friendship.user1Id == friendUserId && friendship.user2Id == currentId))) {
                    friendshipsCollection.document(doc.id).delete().await()
                }
            }
            
            // Decrement friend counts
            decrementFriendCount(currentId)
            decrementFriendCount(friendUserId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove friend", e)
            Result.failure(e)
        }
    }
    
    private suspend fun checkFriendship(userId1: String, userId2: String): Boolean {
        return try {
            val friendships1 = friendshipsCollection
                .whereEqualTo("user1Id", userId1)
                .whereEqualTo("user2Id", userId2)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                .get()
                .await()
            
            if (!friendships1.isEmpty) return true
            
            val friendships2 = friendshipsCollection
                .whereEqualTo("user1Id", userId2)
                .whereEqualTo("user2Id", userId1)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                .get()
                .await()
            
            !friendships2.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun incrementFriendCount(userId: String) {
        usersCollection.document(userId).update(
            "friendsCount", com.google.firebase.firestore.FieldValue.increment(1)
        ).await()
    }
    
    private suspend fun decrementFriendCount(userId: String) {
        usersCollection.document(userId).update(
            "friendsCount", com.google.firebase.firestore.FieldValue.increment(-1)
        ).await()
    }
    
    fun observeFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = friendRequestsCollection
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing friend requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun getFriends(): List<com.example.movietime.data.firebase.FirebaseUser> {
        val userId = currentUserId ?: return emptyList()
        
        return try {
            val friendIds = mutableListOf<String>()
            
            // Get friendships where user is user1
            val friendships1 = friendshipsCollection
                .whereEqualTo("user1Id", userId)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                .get()
                .await()
            friendIds.addAll(friendships1.documents.mapNotNull { 
                it.toObject(Friendship::class.java)?.user2Id 
            })
            
            // Get friendships where user is user2
            val friendships2 = friendshipsCollection
                .whereEqualTo("user2Id", userId)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                .get()
                .await()
            friendIds.addAll(friendships2.documents.mapNotNull { 
                it.toObject(Friendship::class.java)?.user1Id 
            })
            
            // Get user profiles
            if (friendIds.isEmpty()) return emptyList()
            
            friendIds.distinct().mapNotNull { friendId ->
                getUserProfile(friendId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get friends", e)
            emptyList()
        }
    }
    
    fun observeFriends(): Flow<List<com.example.movietime.data.firebase.FirebaseUser>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // This is a simplified version - for real-time updates you'd need a more complex listener
        val friends = getFriends()
        trySend(friends)
        
        awaitClose { }
    }
    
    // ==================== ACTIVITIES ====================
    
    suspend fun shareActivity(
        contentId: Int,
        contentTitle: String,
        contentPoster: String?,
        mediaType: String,
        activityType: ActivityType,
        rating: Float? = null,
        review: String? = null
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val profile = getCurrentUserProfile() ?: return Result.failure(Exception("Profile not found"))
        
        return try {
            val activity = SharedActivity(
                userId = userId,
                userName = profile.displayName,
                userPhoto = profile.photoUrl,
                contentId = contentId,
                contentTitle = contentTitle,
                contentPoster = contentPoster,
                mediaType = mediaType,
                activityType = activityType,
                rating = rating,
                review = review
            )
            
            activitiesCollection.add(activity).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share activity", e)
            Result.failure(e)
        }
    }
    
    fun observeFriendsActivities(): Flow<List<SharedActivity>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Get friends list first
        val friends = getFriends()
        val friendIds = friends.map { it.id }
        
        if (friendIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Firestore whereIn is limited to 10 items, so we chunk the query
        val listener = activitiesCollection
            .whereIn("userId", friendIds.take(10))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing activities", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val activities = snapshot?.toObjects(SharedActivity::class.java) ?: emptyList()
                trySend(activities)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun getUserActivities(userId: String): List<SharedActivity> {
        return try {
            activitiesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
                .toObjects(SharedActivity::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user activities", e)
            emptyList()
        }
    }
    
    suspend fun likeActivity(activityId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val like = ActivityLike(
                activityId = activityId,
                userId = userId
            )
            
            // Add like
            activitiesCollection.document(activityId)
                .collection("likes")
                .document(userId)
                .set(like)
                .await()
            
            // Increment count
            activitiesCollection.document(activityId).update(
                "likesCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to like activity", e)
            Result.failure(e)
        }
    }
    
    suspend fun unlikeActivity(activityId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            activitiesCollection.document(activityId)
                .collection("likes")
                .document(userId)
                .delete()
                .await()
            
            activitiesCollection.document(activityId).update(
                "likesCount", com.google.firebase.firestore.FieldValue.increment(-1)
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unlike activity", e)
            Result.failure(e)
        }
    }
    
    suspend fun commentOnActivity(activityId: String, text: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val profile = getCurrentUserProfile() ?: return Result.failure(Exception("Profile not found"))
        
        return try {
            val comment = ActivityComment(
                activityId = activityId,
                userId = userId,
                userName = profile.displayName,
                userPhoto = profile.photoUrl,
                text = text
            )
            
            activitiesCollection.document(activityId)
                .collection("comments")
                .add(comment)
                .await()
            
            activitiesCollection.document(activityId).update(
                "commentsCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to comment on activity", e)
            Result.failure(e)
        }
    }
    
    // ==================== RECOMMENDATIONS ====================
    
    suspend fun sendRecommendation(
        toUserId: String,
        contentId: Int,
        contentTitle: String,
        contentPoster: String?,
        mediaType: String,
        message: String = ""
    ): Result<Unit> {
        val fromUserId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val profile = getCurrentUserProfile() ?: return Result.failure(Exception("Profile not found"))
        
        return try {
            val recommendation = Recommendation(
                fromUserId = fromUserId,
                toUserId = toUserId,
                fromUserName = profile.displayName,
                fromUserPhoto = profile.photoUrl,
                contentId = contentId,
                contentTitle = contentTitle,
                contentPoster = contentPoster,
                mediaType = mediaType,
                message = message
            )
            
            recommendationsCollection.add(recommendation).await()
            
            sendNotification(
                toUserId,
                NotificationType.NEW_RECOMMENDATION,
                "Нова рекомендація",
                "${profile.displayName} рекомендує вам: $contentTitle",
                mapOf(
                    "contentId" to contentId.toString(),
                    "mediaType" to mediaType
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send recommendation", e)
            Result.failure(e)
        }
    }
    
    fun observeRecommendations(): Flow<List<Recommendation>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = recommendationsCollection
            .whereEqualTo("toUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing recommendations", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val recommendations = snapshot?.toObjects(Recommendation::class.java) ?: emptyList()
                trySend(recommendations)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun markRecommendationAsRead(recommendationId: String): Result<Unit> {
        return try {
            recommendationsCollection.document(recommendationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== NOTIFICATIONS ====================
    
    private suspend fun sendNotification(
        toUserId: String,
        type: NotificationType,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        try {
            val notification = FirebaseNotification(
                userId = toUserId,
                type = type,
                title = title,
                message = message,
                data = data
            )
            notificationsCollection.add(notification).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
    
    fun observeNotifications(): Flow<List<FirebaseNotification>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(FirebaseNotification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUnreadNotificationsCount(): Int {
        val userId = currentUserId ?: return 0
        return try {
            notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }
    
    // ==================== SYNC USER STATS ====================
    
    suspend fun syncUserStats(
        watchedMovies: Int,
        watchedTvShows: Int,
        totalWatchTimeMinutes: Int,
        averageRating: Float
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "totalWatchedMovies" to watchedMovies,
                    "totalWatchedTvShows" to watchedTvShows,
                    "totalWatchTimeMinutes" to totalWatchTimeMinutes,
                    "averageRating" to averageRating
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user stats", e)
            Result.failure(e)
        }
    }
}
