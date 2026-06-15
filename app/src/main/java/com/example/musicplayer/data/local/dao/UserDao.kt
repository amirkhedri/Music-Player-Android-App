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
        WHERE email = :email
          AND password_hash = :passwordHash
        LIMIT 1
        """
    )
    suspend fun getUserByCredentials(email: String, passwordHash: String): UserEntity?

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    fun getUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun emailExists(email: String): Int
}