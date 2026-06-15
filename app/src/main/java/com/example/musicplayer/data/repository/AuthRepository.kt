package com.example.musicplayer.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.musicplayer.data.local.SessionManager
import com.example.musicplayer.data.local.dao.UserDao
import com.example.musicplayer.data.local.entity.UserEntity
import com.example.musicplayer.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    val loggedInUserId: Flow<Long> = sessionManager.loggedInUserId

    suspend fun register(displayName: String, email: String, passwordRaw: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val hash = PasswordUtils.hash(email, passwordRaw)
                val newUser = UserEntity(
                    displayName = displayName,
                    email = email.lowercase().trim(),
                    passwordHash = hash
                )
                val newUserId = userDao.insertUser(newUser)
                sessionManager.saveSession(newUserId)
                true
            } catch (e: SQLiteConstraintException) {
                false
            }
        }
    }

    suspend fun login(email: String, passwordRaw: String): Boolean {
        return withContext(Dispatchers.IO) {
            val formattedEmail = email.lowercase().trim()
            val hash = PasswordUtils.hash(formattedEmail, passwordRaw)
            val user = userDao.getUserByCredentials(formattedEmail, hash)

            if (user != null) {
                sessionManager.saveSession(user.userId)
                true
            } else {
                false
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            sessionManager.clearSession()
        }
    }
}