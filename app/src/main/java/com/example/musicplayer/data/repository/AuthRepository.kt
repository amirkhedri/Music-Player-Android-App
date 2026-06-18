package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.SessionManager
import com.example.musicplayer.data.local.dao.UserDao
import com.example.musicplayer.data.local.entity.UserEntity
import com.example.musicplayer.util.PasswordUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    val loggedInUserId: Flow<Long> = sessionManager.loggedInUserId

    suspend fun login(username: String, passwordRaw: String): Boolean {
        val hash = PasswordUtils.hash(username, passwordRaw)
        val user = userDao.getUserByCredentials(username, hash)
        return if (user != null) {
            sessionManager.saveSession(user.userId)
            true
        } else false
    }

    suspend fun register(username: String, passwordRaw: String): Boolean {
        if (userDao.userExists(username) > 0) return false
        val hash = PasswordUtils.hash(username, passwordRaw)
        val user = UserEntity(username = username, passwordHash = hash)
        val id = userDao.insertUser(user)
        sessionManager.saveSession(id)
        return true
    }

    suspend fun resetPassword(username: String, newPasswordRaw: String): Boolean {
        if (userDao.userExists(username) == 0) return false
        val newHash = PasswordUtils.hash(username, newPasswordRaw)
        userDao.updatePassword(username, newHash)
        return true
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }
}