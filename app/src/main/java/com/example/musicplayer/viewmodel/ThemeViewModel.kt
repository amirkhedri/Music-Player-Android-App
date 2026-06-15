package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// NEW: This single global object ensures every screen shares the exact same switch
object ThemeState {
    val isDarkMode = MutableStateFlow(true) // Set to true if you want Dark Mode by default!
}

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = ThemeState.isDarkMode.asStateFlow()

    fun toggleTheme() {
        ThemeState.isDarkMode.value = !ThemeState.isDarkMode.value
    }
}