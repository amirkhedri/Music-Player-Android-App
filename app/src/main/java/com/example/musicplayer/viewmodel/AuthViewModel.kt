package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val loggedInUserId: StateFlow<Long> = authRepository.loggedInUserId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = -1L
        )

    suspend fun login(username: String, passwordRaw: String): Boolean {
        return authRepository.login(username, passwordRaw)
    }

    suspend fun register(username: String, passwordRaw: String): Boolean {
        return authRepository.register(username, passwordRaw)
    }

    suspend fun resetPassword(username: String, newPasswordRaw: String): Boolean {
        return authRepository.resetPassword(username, newPasswordRaw)
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}