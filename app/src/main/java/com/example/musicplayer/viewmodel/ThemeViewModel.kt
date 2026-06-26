package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class AppThemeMode { LIGHT, DARK, GLASSY }

object ThemeState {
    val isDarkMode = MutableStateFlow(true)
    val isGlassyMode = MutableStateFlow(true)
}

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = ThemeState.isDarkMode.asStateFlow()
    val isGlassyMode: StateFlow<Boolean> = ThemeState.isGlassyMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        when (mode) {
            AppThemeMode.LIGHT -> {
                ThemeState.isDarkMode.value = false
                ThemeState.isGlassyMode.value = false
            }
            AppThemeMode.DARK -> {
                ThemeState.isDarkMode.value = true
                ThemeState.isGlassyMode.value = false
            }
            AppThemeMode.GLASSY -> {
                ThemeState.isDarkMode.value = true
                ThemeState.isGlassyMode.value = true
            }
        }
    }

    fun cycleTheme() {
        val currentMode = when {
            ThemeState.isGlassyMode.value -> AppThemeMode.GLASSY
            ThemeState.isDarkMode.value -> AppThemeMode.DARK
            else -> AppThemeMode.LIGHT
        }
        val nextMode = when (currentMode) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.GLASSY
            AppThemeMode.GLASSY -> AppThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }
}