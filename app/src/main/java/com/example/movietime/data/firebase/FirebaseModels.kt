package com.example.movietime.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * User profile stored in Firestore
 */
data class FirebaseUser(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val isPublic: Boolean = true,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastActive: Timestamp? = null,
    
    // Stats
    val totalWatchedMovies: Int = 0,
    val totalWatchedTvShows: Int = 0,
    val totalWatchTimeMinutes: Int = 0,
    val averageRating: Float = 0f,
    
    // Social
    val friendsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    // No-arg constructor for Firestore
    constructor() : this("")
}

/**
 * Friendship relation
 */
data class Friendship(
    @DocumentId
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val status: FriendshipStatus = FriendshipStatus.PENDING
) {
    constructor() : this("")
}

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}

/**
 * Friend request
 */
data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    val message: String = ""
) {
    constructor() : this("")
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

/**
 * Activity shared with friends
 */
data class SharedActivity(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String? = null,
    
    // Content info
    val contentId: Int = 0,
    val contentTitle: String = "",
    val contentPoster: String? = null,
    val mediaType: String = "", // "movie" or "tv"
    
    // Activity type
    val activityType: ActivityType = ActivityType.WATCHED,
    val rating: Float? = null,
    val review: String? = null,
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    // Engagement
    val likesCount: Int = 0,
    val commentsCount: Int = 0
) {
    constructor() : this("")
}

enum class ActivityType {
    WATCHED,
    WATCHING,
    PLANNED,
    RATED,
    REVIEWED,
    RECOMMENDED
}

/**
 * Comment on activity
 */
data class ActivityComment(
    @DocumentId
    val id: String = "",
    val activityId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String? = null,
    val text: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    constructor() : this("")
}

/**
 * Like on activity
 */
data class ActivityLike(
    @DocumentId
    val id: String = "",
    val activityId: String = "",
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    constructor() : this("")
}

/**
 * Movie/TV recommendation sent to a friend
 */
data class Recommendation(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String? = null,
    
    // Content
    val contentId: Int = 0,
    val contentTitle: String = "",
    val contentPoster: String? = null,
    val mediaType: String = "",
    
    val message: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val isRead: Boolean = false
) {
    constructor() : this("")
}

/**
 * Notification
 */
data class FirebaseNotification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.FRIEND_REQUEST,
    val title: String = "",
    val message: String = "",
    val data: Map<String, String> = emptyMap(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val isRead: Boolean = false
) {
    constructor() : this("")
}

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    NEW_RECOMMENDATION,
    ACTIVITY_LIKE,
    ACTIVITY_COMMENT,
    FRIEND_ACTIVITY
}
