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

    // Exposes the logged-in user ID as a StateFlow so Compose UI can react instantly
    val loggedInUserId: StateFlow<Long> = authRepository.loggedInUserId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = -1L
        )

    suspend fun login(email: String, passwordRaw: String): Boolean {
        return authRepository.login(email, passwordRaw)
    }

    suspend fun register(displayName: String, email: String, passwordRaw: String): Boolean {
        return authRepository.register(displayName, email, passwordRaw)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}