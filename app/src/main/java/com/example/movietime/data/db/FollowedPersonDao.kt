package com.example.movietime.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FollowedPersonDao {
    
    @Query("SELECT * FROM followed_people ORDER BY followedAt DESC")
    fun getAllFollowed(): LiveData<List<FollowedPerson>>
    
    @Query("SELECT * FROM followed_people ORDER BY followedAt DESC")
    suspend fun getAllFollowedSync(): List<FollowedPerson>
    
    @Query("SELECT * FROM followed_people WHERE notificationsEnabled = 1")
    suspend fun getFollowedWithNotifications(): List<FollowedPerson>
    
    @Query("SELECT * FROM followed_people WHERE personId = :personId")
    suspend fun getById(personId: Int): FollowedPerson?
    
    @Query("SELECT EXISTS(SELECT 1 FROM followed_people WHERE personId = :personId)")
    suspend fun isFollowing(personId: Int): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM followed_people WHERE personId = :personId)")
    fun isFollowingLive(personId: Int): LiveData<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: FollowedPerson)
    
    @Delete
    suspend fun delete(person: FollowedPerson)
    
    @Query("DELETE FROM followed_people WHERE personId = :personId")
    suspend fun deleteById(personId: Int)
    
    @Query("UPDATE followed_people SET notificationsEnabled = :enabled WHERE personId = :personId")
    suspend fun setNotificationsEnabled(personId: Int, enabled: Boolean)
    
    @Query("SELECT COUNT(*) FROM followed_people")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM followed_people WHERE knownForDepartment = :department")
    suspend fun getCountByDepartment(department: String): Int
}
