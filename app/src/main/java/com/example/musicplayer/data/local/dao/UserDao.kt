package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query(
        """
        SELECT * FROM users
        WHERE username = :username
          AND password_hash = :passwordHash
        LIMIT 1
        """
    )
    suspend fun getUserByCredentials(username: String, passwordHash: String): UserEntity?

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    fun getUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun userExists(username: String): Int

    // NEW: Query to update a forgotten password
    @Query("UPDATE users SET password_hash = :newPasswordHash WHERE username = :username")
    suspend fun updatePassword(username: String, newPasswordHash: String)
}